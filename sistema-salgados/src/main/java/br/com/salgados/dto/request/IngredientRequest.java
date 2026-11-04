package br.com.salgados.dto.request;

import java.math.BigDecimal;

import br.com.salgados.domain.enums.Unit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record IngredientRequest(
        @NotBlank(message = "Informe o nome do ingrediente.")
        @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres.")
        String name,

        @Size(max = 50, message = "A categoria pode ter no máximo 50 caracteres.")
        String category,

        @NotNull(message = "Informe a unidade-base (G, ML ou UN).")
        Unit baseUnit,

        boolean allowFraction,

        @NotNull(message = "Informe o estoque mínimo (use 0 se não houver).")
        @PositiveOrZero(message = "O estoque mínimo não pode ser negativo.")
        BigDecimal minimumStock,

        /** Unidade em que o estoque mínimo foi digitado (ex.: KG). Se omitida, usa a unidade-base. */
        Unit minimumStockUnit) {
}