package br.com.salgados.dto.request;

import jakarta.validation.constraints.NotNull;

public record IngredientStatusRequest(
        @NotNull(message = "Informe se o ingrediente deve ficar ativo.")
        Boolean active) {
}