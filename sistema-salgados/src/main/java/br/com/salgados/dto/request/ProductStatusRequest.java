package br.com.salgados.dto.request;

import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(
        @NotNull(message = "Informe se o produto deve ficar ativo.")
        Boolean active) {
}