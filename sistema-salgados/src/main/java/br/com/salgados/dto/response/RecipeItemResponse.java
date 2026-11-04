package br.com.salgados.dto.response;

import java.math.BigDecimal;

import br.com.salgados.domain.enums.Unit;

/** displayQuantity/displayUnit: como o usuário digitou. quantityBase/baseUnit: como o sistema calcula. */
public record RecipeItemResponse(
        Long ingredientId,
        String ingredientName,
        Unit baseUnit,
        BigDecimal quantityBase,
        BigDecimal displayQuantity,
        Unit displayUnit) {
}