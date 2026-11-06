package br.com.salgados.dto.response;

import java.math.BigDecimal;

public record ReconciliationEntry(
        Long ingredientId,
        String ingredientName,
        BigDecimal storedStock,
        BigDecimal expectedStock,
        BigDecimal storedAverageCost,
        BigDecimal expectedAverageCost) {
}