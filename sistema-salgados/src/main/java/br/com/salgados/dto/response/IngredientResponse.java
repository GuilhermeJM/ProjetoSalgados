package br.com.salgados.dto.response;

import java.math.BigDecimal;

import br.com.salgados.domain.enums.Unit;
import br.com.salgados.domain.model.Ingredient;

public record IngredientResponse(
        Long id,
        String name,
        String category,
        Unit baseUnit,
        boolean allowFraction,
        BigDecimal minimumStock,
        BigDecimal currentStock,
        BigDecimal currentAverageCost,
        boolean active,
        boolean belowMinimum) {

    public static IngredientResponse from(Ingredient i) {
        boolean below = i.active() && i.currentStock().compareTo(i.minimumStock()) < 0;
        return new IngredientResponse(i.id(), i.name(), i.category(), i.baseUnit(), i.allowFraction(),
                i.minimumStock(), i.currentStock(), i.currentAverageCost(), i.active(), below);
    }
}