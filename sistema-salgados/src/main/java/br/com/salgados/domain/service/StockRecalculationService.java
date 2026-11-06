package br.com.salgados.domain.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.salgados.domain.enums.MovementType;
import br.com.salgados.domain.model.Ingredient;
import br.com.salgados.domain.model.PurchaseLineCost;
import br.com.salgados.domain.model.StockMovement;
import br.com.salgados.dto.response.ReconciliationEntry;
import br.com.salgados.dto.response.ReconciliationResponse;
import br.com.salgados.exception.BusinessException;
import br.com.salgados.repository.IngredientRepository;
import br.com.salgados.repository.PurchaseRepository;
import br.com.salgados.repository.StockMovementRepository;

/**
 * Único lugar que calcula saldo e custo médio de um ingrediente: reprocessa o extrato em ordem
 * (data do movimento, depois ordem de gravação).
 * Regras: compra entra pela fórmula do custo médio ponderado; qualquer outro movimento só muda
 * o saldo e deixa o custo médio como está.
 */
@Service
public class StockRecalculationService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** firstNegativeAt é a data do primeiro ponto em que o saldo ficou negativo (null se nunca ficou). */
    public record Result(BigDecimal stock, BigDecimal averageCost, LocalDateTime firstNegativeAt) {
    }

    private final StockMovementRepository movements;
    private final PurchaseRepository purchases;
    private final IngredientRepository ingredients;
    private final CostCalculationService costs;

    public StockRecalculationService(StockMovementRepository movements, PurchaseRepository purchases,
                                     IngredientRepository ingredients, CostCalculationService costs) {
        this.movements = movements;
        this.purchases = purchases;
        this.ingredients = ingredients;
        this.costs = costs;
    }

    public static String negativeStockMessage(LocalDateTime when) {
        return "Com essa data, o saldo ficaria negativo em " + when.format(DATE) + ". "
                + "Use uma data posterior às entradas de estoque ou reduza a quantidade.";
    }

    /** Calcula sem gravar nada. */
    public Result calculate(long ingredientId) {
        return simulate(ingredientId, List.of());
    }

    /** Calcula como o extrato ficaria se os movimentos `extra` fossem acrescentados (sem gravar). */
    public Result simulate(long ingredientId, List<StockMovement> extra) {
        List<StockMovement> timeline = new ArrayList<>(movements.findAllByIngredient(ingredientId));
        timeline.addAll(extra);
        // movimentos ainda sem id (simulados) ficam por último dentro do mesmo instante
        timeline.sort(Comparator.comparing(StockMovement::occurredAt)
                .thenComparing(m -> m.id() == null ? Long.MAX_VALUE : m.id()));

        // custo de cada linha de compra, agrupado por compra e consumido na ordem de gravação
        Map<Long, Deque<PurchaseLineCost>> lines = new HashMap<>();
        for (PurchaseLineCost line : purchases.findLineCosts(ingredientId)) {
            lines.computeIfAbsent(line.purchaseId(), k -> new ArrayDeque<>()).add(line);
        }

        BigDecimal stock = BigDecimal.ZERO;
        BigDecimal average = BigDecimal.ZERO;
        LocalDateTime firstNegativeAt = null;

        for (StockMovement m : timeline) {
            if (m.movementType() == MovementType.PURCHASE) {
                PurchaseLineCost line = lines.getOrDefault(m.sourceId(), new ArrayDeque<>()).poll();
                if (line == null || line.quantityBase().compareTo(m.quantity()) != 0) {
                    throw new IllegalStateException("Extrato inconsistente: o movimento " + m.id()
                            + " é uma entrada de compra sem item de compra correspondente.");
                }
                average = costs.newAverageCost(stock, average, m.quantity(), line.totalCost());
            }
            stock = stock.add(m.quantity());
            if (stock.signum() < 0 && firstNegativeAt == null) {
                firstNegativeAt = m.occurredAt();
            }
        }
        return new Result(stock, average, firstNegativeAt);
    }

    /** Recalcula e grava saldo e custo médio no ingrediente. Recusa se a história ficar negativa. */
    @Transactional
    public Result apply(long ingredientId) {
        Result result = calculate(ingredientId);
        if (result.firstNegativeAt() != null) {
            throw new BusinessException("NEGATIVE_STOCK_IN_HISTORY",
                    negativeStockMessage(result.firstNegativeAt()));
        }
        ingredients.updateStockAndCost(ingredientId, result.stock(), result.averageCost());
        return result;
    }

    /**
     * Confere o saldo e o custo médio guardados contra o que o extrato indica.
     * Com fix = true, grava os valores do extrato nos ingredientes divergentes.
     */
    @Transactional
    public ReconciliationResponse reconcile(boolean fix) {
        List<Ingredient> all = ingredients.search(null, null, null);
        List<ReconciliationEntry> divergences = new ArrayList<>();

        for (Ingredient i : all) {
            Result expected = calculate(i.id());
            boolean same = i.currentStock().compareTo(expected.stock()) == 0
                    && i.currentAverageCost().compareTo(expected.averageCost()) == 0;
            if (!same) {
                divergences.add(new ReconciliationEntry(i.id(), i.name(), i.currentStock(),
                        expected.stock(), i.currentAverageCost(), expected.averageCost()));
                if (fix) {
                    ingredients.updateStockAndCost(i.id(), expected.stock(), expected.averageCost());
                }
            }
        }
        return new ReconciliationResponse(all.size(), fix, divergences);
    }
}