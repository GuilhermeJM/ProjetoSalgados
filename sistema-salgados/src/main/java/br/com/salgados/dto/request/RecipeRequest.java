package br.com.salgados.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RecipeRequest(
        /** Quantas unidades um lote desta receita rende. */
        @NotNull(message = "Informe o rendimento da receita.")
        @Positive(message = "O rendimento deve ser maior que zero.")
        BigDecimal yieldQuantity,

        @NotNull(message = "Informe os ingredientes da receita.")
        @Valid
        List<RecipeItemRequest> items) {
}