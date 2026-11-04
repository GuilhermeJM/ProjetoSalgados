package br.com.salgados.domain.model;

import java.math.BigDecimal;

import br.com.salgados.domain.enums.Unit;

/**
 * quantityBase está na unidade-base do ingrediente (g, ml ou un) e é por lote da receita.
 * displayUnit é só a unidade em que o usuário digitou (ex.: KG), para exibir como ele escreveu.
 * ingredientName e baseUnit vêm do JOIN na leitura.
 */
public record RecipeItem(
        Long id,
        Long recipeId,
        Long ingredientId,
        String ingredientName,
        Unit baseUnit,
        BigDecimal quantityBase,
        Unit displayUnit) {
}