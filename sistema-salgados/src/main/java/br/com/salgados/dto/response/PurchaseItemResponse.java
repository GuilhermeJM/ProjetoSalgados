package br.com.salgados.dto.response;

import java.math.BigDecimal;

import br.com.salgados.domain.enums.Unit;

public record PurchaseItemResponse(
        Long ingredientId,
        String ingredientName,
        Unit baseUnit,
        BigDecimal quantityBase,
        BigDecimal totalCost,
        BigDecimal unitCostBase) {
}