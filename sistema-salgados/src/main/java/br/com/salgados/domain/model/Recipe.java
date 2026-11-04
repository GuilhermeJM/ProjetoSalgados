package br.com.salgados.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.salgados.domain.enums.RecipeStatus;

/** yieldQuantity é o rendimento de um lote da receita, em unidades. */
public record Recipe(
        Long id,
        Long productId,
        int version,
        BigDecimal yieldQuantity,
        String yieldUnit,
        RecipeStatus status,
        LocalDateTime createdAt) {
}