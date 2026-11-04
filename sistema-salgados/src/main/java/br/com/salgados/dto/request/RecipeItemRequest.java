package br.com.salgados.dto.request;

import java.math.BigDecimal;

import br.com.salgados.domain.enums.Unit;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RecipeItemRequest(
        @NotNull(message = "Selecione o ingrediente.")
        Long ingredientId,

        @NotNull(message = "Informe a quantidade.")
        @Positive(message = "A quantidade deve ser maior que zero.")
        BigDecimal quantity,

        @NotNull(message = "Informe a unidade.")
        Unit unit) {
}