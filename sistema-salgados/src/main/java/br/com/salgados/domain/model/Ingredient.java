package br.com.salgados.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.salgados.domain.enums.Unit;

/** Quantidades e custo sempre na unidade-base (g, ml ou un). */
public record Ingredient(
        Long id,
        String name,
        String category,
        Unit baseUnit,
        boolean allowFraction,
        BigDecimal minimumStock,
        BigDecimal currentStock,
        BigDecimal currentAverageCost,
        boolean active,
        LocalDateTime createdAt) {
}