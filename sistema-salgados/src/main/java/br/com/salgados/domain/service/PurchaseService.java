package br.com.salgados.domain.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.salgados.domain.enums.MovementType;
import br.com.salgados.domain.enums.SourceType;
import br.com.salgados.domain.model.Ingredient;
import br.com.salgados.domain.model.Purchase;
import br.com.salgados.domain.model.StockMovement;
import br.com.salgados.dto.request.PurchaseItemRequest;
import br.com.salgados.dto.request.PurchaseRequest;
import br.com.salgados.dto.response.PurchaseItemResponse;
import br.com.salgados.dto.response.PurchaseResponse;
import br.com.salgados.exception.BusinessException;
import br.com.salgados.exception.NotFoundException;
import br.com.salgados.repository.IngredientRepository;
import br.com.salgados.repository.PurchaseRepository;
import br.com.salgados.repository.StockMovementRepository;

@Service
public class PurchaseService {

    /** Linha já validada e convertida para a unidade-base. */
    private record Line(long ingredientId, BigDecimal quantityBase, BigDecimal totalCost) {
    }

    private final PurchaseRepository purchases;
    private final IngredientRepository ingredients;
    private final StockMovementRepository movements;
    private final UnitConversionService conversion;
    private final CostCalculationService costs;

    public PurchaseService(PurchaseRepository purchases, IngredientRepository ingredients,
                           StockMovementRepository movements, UnitConversionService conversion,
                           CostCalculationService costs) {
        this.purchases = purchases;
        this.ingredients = ingredients;
        this.movements = movements;
        this.conversion = conversion;
        this.costs = costs;
    }

    public List<PurchaseResponse> list(LocalDate from, LocalDate to, Long ingredientId) {
        return purchases.search(from, to, ingredientId).stream().map(this::toResponse).toList();
    }

    public PurchaseResponse findById(long id) {
        return toResponse(get(id));
    }

    @Transactional
    public PurchaseResponse register(PurchaseRequest request) {
        if (request.purchaseDate().isAfter(LocalDate.now())) {
            throw new BusinessException("PURCHASE_DATE_IN_FUTURE", "A data da compra não pode ser futura.");
        }
        if (request.items().isEmpty()) {
            throw new BusinessException("PURCHASE_WITHOUT_ITEMS", "A compra precisa ter pelo menos um item.");
        }

        // 1) validar tudo, sem gravar nada
        List<Line> lines = new ArrayList<>();
        for (PurchaseItemRequest item : request.items()) {
            Ingredient ingredient = ingredients.findById(item.ingredientId()).orElseThrow(() ->
                    new BusinessException("INGREDIENT_NOT_FOUND",
                            "Ingrediente não encontrado (id " + item.ingredientId() + ")."));
            if (!ingredient.active()) {
                throw new BusinessException("INGREDIENT_INACTIVE",
                        "O ingrediente \"" + ingredient.name() + "\" está inativo. "
                        + "Reative-o ou retire-o da compra.");
            }
            BigDecimal quantityBase = conversion.toBase(item.quantity(), item.unit(), ingredient.baseUnit());
            conversion.validateQuantity(quantityBase, ingredient.baseUnit(), ingredient.allowFraction());

            lines.add(new Line(ingredient.id(), quantityBase, item.totalCost()));
        }

        // 2) gravar compra, itens, movimentos e saldos (tudo na mesma transação)
        BigDecimal total = lines.stream().map(Line::totalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime now = LocalDateTime.now();
        long purchaseId = purchases.insert(new Purchase(null, request.purchaseDate(),
                normalize(request.notes()), total, now));
        LocalDateTime occurredAt = request.purchaseDate().atStartOfDay();

        for (Line line : lines) {
            // Relê o ingrediente: se ele aparece em duas linhas, a segunda enxerga o saldo já atualizado.
            Ingredient current = ingredients.findById(line.ingredientId()).orElseThrow();

            BigDecimal newStock = current.currentStock().add(line.quantityBase());
            BigDecimal newAverage = costs.newAverageCost(current.currentStock(), current.currentAverageCost(),
                    line.quantityBase(), line.totalCost());

            purchases.insertItem(purchaseId, line.ingredientId(), line.quantityBase(), line.totalCost(),
                    costs.unitCost(line.totalCost(), line.quantityBase()));
            movements.insert(new StockMovement(null, line.ingredientId(), MovementType.PURCHASE,
                    line.quantityBase(), SourceType.PURCHASE, purchaseId, occurredAt, null, now));
            ingredients.updateStockAndCost(line.ingredientId(), newStock, newAverage);
        }

        return findById(purchaseId);
    }

    // ---------- auxiliares ----------

    private Purchase get(long id) {
        return purchases.findById(id).orElseThrow(() ->
                new NotFoundException("PURCHASE_NOT_FOUND", "Compra não encontrada."));
    }

    private PurchaseResponse toResponse(Purchase p) {
        List<PurchaseItemResponse> items = purchases.findItems(p.id()).stream()
                .map(i -> new PurchaseItemResponse(i.ingredientId(), i.ingredientName(), i.baseUnit(),
                        i.quantityBase(), i.totalCost(), i.unitCostBase()))
                .toList();
        return new PurchaseResponse(p.id(), p.purchaseDate(), p.notes(), p.totalAmount(),
                p.createdAt(), items);
    }

    private static String normalize(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}