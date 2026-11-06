package br.com.salgados.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.salgados.domain.enums.AdjustmentDirection;
import br.com.salgados.domain.enums.Unit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StockAdjustmentRequest(
        @NotNull(message = "Selecione o ingrediente.")
        Long ingredientId,

        @NotNull(message = "Informe se o ajuste aumenta ou diminui o estoque.")
        AdjustmentDirection direction,

        /** Sempre positiva; o sentido vem de `direction`. */
        @NotNull(message = "Informe a quantidade.")
        @Positive(message = "A quantidade deve ser maior que zero.")
        BigDecimal quantity,

        @NotNull(message = "Informe a unidade.")
        Unit unit,

        @NotNull(message = "Informe a data do ajuste.")
        LocalDate occurredOn,

        @NotBlank(message = "Informe a justificativa do ajuste.")
        @Size(max = 300, message = "A justificativa pode ter no máximo 300 caracteres.")
        String reason) {
}