package br.com.salgados.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.salgados.domain.enums.Unit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StockLossRequest(
        @NotNull(message = "Selecione o ingrediente.")
        Long ingredientId,

        @NotNull(message = "Informe a quantidade perdida.")
        @Positive(message = "A quantidade deve ser maior que zero.")
        BigDecimal quantity,

        @NotNull(message = "Informe a unidade.")
        Unit unit,

        @NotNull(message = "Informe a data da perda.")
        LocalDate occurredOn,

        @NotBlank(message = "Informe o motivo da perda.")
        @Size(max = 300, message = "O motivo pode ter no máximo 300 caracteres.")
        String reason) {
}