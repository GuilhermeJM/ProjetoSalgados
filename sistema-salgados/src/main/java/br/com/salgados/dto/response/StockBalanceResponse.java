package br.com.salgados.dto.response;

import java.math.BigDecimal;

import br.com.salgados.domain.enums.Unit;

public record StockBalanceResponse(
        Long id,
        String name,
        String category,
        Unit baseUnit,
        boolean active,
        BigDecimal currentStock,
        BigDecimal minimumStock,
        BigDecimal averageCost,
        BigDecimal stockValue,
        boolean belowMinimum) {
}