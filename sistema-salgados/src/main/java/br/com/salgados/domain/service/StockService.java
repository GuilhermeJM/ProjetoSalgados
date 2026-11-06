package br.com.salgados.domain.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.salgados.domain.enums.AdjustmentDirection;
import br.com.salgados.domain.enums.MovementType;
import br.com.salgados.domain.enums.SourceType;
import br.com.salgados.domain.enums.Unit;
import br.com.salgados.domain.model.Ingredient;
import br.com.salgados.domain.model.StockMovement;
import br.com.salgados.dto.request.StockAdjustmentRequest;
import br.com.salgados.dto.request.StockLossRequest;
import br.com.salgados.dto.response.ReconciliationResponse;
import br.com.salgados.dto.response.StockBalanceResponse;
import br.com.salgados.dto.response.StockMovementResponse;
import br.com.salgados.exception.BusinessException;
import br.com.salgados.repository.IngredientRepository;
import br.com.salgados.repository.StockMovementRepository;

@Service
public class StockService {

    private static final int DEFAULT_LIMIT = 500;
    private static final int MAX_LIMIT = 2000;

    private final IngredientRepository ingredients;
    private final StockMovementRepository movements;
    private final UnitConversionService conversion;
    private final CostCalculationService costs;
    private final StockRecalculationService recalculation;

    public StockService(IngredientRepository ingredients, StockMovementRepository movements,
                        UnitConversionService conversion, CostCalculationService costs,
                        StockRecalculationService recalculation) {
        this.ingredients = ingredients;
        this.movements = movements;
        this.conversion = conversion;
        this.costs = costs;
        this.recalculation = recalculation;
    }

    // ---------- consultas ----------

    public List<StockBalanceResponse> balance(String search, String category, Boolean active,
                                              Boolean belowMinimum) {
        return ingredients.search(search, category, active).stream()
                .map(this::toBalance)
                .filter(b -> !Boolean.TRUE.equals(belowMinimum) || b.belowMinimum())
                .toList();
    }

    /** Extrato, do mais recente para o mais antigo. O limite padrão é 500 e o máximo é 2000. */
    public List<StockMovementResponse> listMovements(Long ingredientId, MovementType type,
                                                     LocalDate from, LocalDate to, Integer limit) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("INVALID_PERIOD", "A data inicial não pode ser posterior à data final.");
        }
        int effectiveLimit = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
        return movements.search(ingredientId, type, from, to, effectiveLimit).stream()
                .map(StockMovementResponse::from)
                .toList();
    }

    // ---------- ajuste e perda ----------

    @Transactional
    public StockMovementResponse adjust(StockAdjustmentRequest request) {
        requireReason(request.reason());
        if (request.direction() == null) {
            throw new BusinessException("DIRECTION_REQUIRED",
                    "Informe se o ajuste aumenta ou diminui o estoque.");
        }
        Ingredient ingredient = getIngredient(request.ingredientId());
        BigDecimal quantity = toBaseQuantity(ingredient, request.quantity(), request.unit());
        BigDecimal signed = request.direction() == AdjustmentDirection.DECREASE
                ? quantity.negate() : quantity;
        return register(ingredient, MovementType.ADJUSTMENT, signed, request.occurredOn(), request.reason());
    }

    @Transactional
    public StockMovementResponse registerLoss(StockLossRequest request) {
        requireReason(request.reason());
        Ingredient ingredient = getIngredient(request.ingredientId());
        BigDecimal quantity = toBaseQuantity(ingredient, request.quantity(), request.unit());
        return register(ingredient, MovementType.LOSS, quantity.negate(), request.occurredOn(), request.reason());
    }

    // ---------- reconciliação ----------

    public ReconciliationResponse reconcile(boolean fix) {
        return recalculation.reconcile(fix);
    }

    // ---------- auxiliares ----------

    private StockMovementResponse register(Ingredient ingredient, MovementType type, BigDecimal signed,
                                           LocalDate date, String reason) {
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessException("MOVEMENT_DATE_IN_FUTURE", "A data não pode ser futura.");
        }

        String unit = ingredient.baseUnit().name().toLowerCase(Locale.ROOT);
        if (signed.signum() < 0 && ingredient.currentStock().add(signed).signum() < 0) {
            throw new BusinessException("INSUFFICIENT_STOCK",
                    "Estoque insuficiente de \"" + ingredient.name() + "\": há " + plain(ingredient.currentStock())
                    + " " + unit + " e a saída pedida é de " + plain(signed.negate()) + " " + unit + ".");
        }

        StockMovement candidate = new StockMovement(null, ingredient.id(), type, signed, SourceType.MANUAL,
                null, date.atStartOfDay(), reason.trim(), LocalDateTime.now());

        // Simula antes de gravar: com data retroativa, o saldo não pode ficar negativo em ponto nenhum.
        StockRecalculationService.Result preview = recalculation.simulate(ingredient.id(), List.of(candidate));
        if (preview.firstNegativeAt() != null) {
            throw new BusinessException("NEGATIVE_STOCK_IN_HISTORY",
                    StockRecalculationService.negativeStockMessage(preview.firstNegativeAt()));
        }

        long id = movements.insert(candidate);
        recalculation.apply(ingredient.id());
        return StockMovementResponse.from(movements.findDetailById(id).orElseThrow());
    }

    private Ingredient getIngredient(Long id) {
        return ingredients.findById(id).orElseThrow(() ->
                new BusinessException("INGREDIENT_NOT_FOUND", "Ingrediente não encontrado (id " + id + ")."));
    }

    private BigDecimal toBaseQuantity(Ingredient ingredient, BigDecimal quantity, Unit unit) {
        BigDecimal base = conversion.toBase(quantity, unit, ingredient.baseUnit());
        conversion.validateQuantity(base, ingredient.baseUnit(), ingredient.allowFraction());
        return base;
    }

    private static void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("REASON_REQUIRED",
                    "Informe a justificativa. Ela é obrigatória para ajustes e perdas de estoque.");
        }
    }

    private StockBalanceResponse toBalance(Ingredient i) {
        boolean below = i.active() && i.currentStock().compareTo(i.minimumStock()) < 0;
        return new StockBalanceResponse(i.id(), i.name(), i.category(), i.baseUnit(), i.active(),
                i.currentStock(), i.minimumStock(), i.currentAverageCost(),
                costs.stockValue(i.currentStock(), i.currentAverageCost()), below);
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}