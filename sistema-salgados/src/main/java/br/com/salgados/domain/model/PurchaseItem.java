package br.com.salgados.domain.model;

import java.math.BigDecimal;

import br.com.salgados.domain.enums.Unit;

/** ingredientName e baseUnit vêm do JOIN na leitura. unitCostBase é o custo por g/ml/un desta compra. */
public record PurchaseItem(
        Long id,
        Long purchaseId,
        Long ingredientId,
        String ingredientName,
        Unit baseUnit,
        BigDecimal quantityBase,
        BigDecimal totalCost,
        BigDecimal unitCostBase) {
}