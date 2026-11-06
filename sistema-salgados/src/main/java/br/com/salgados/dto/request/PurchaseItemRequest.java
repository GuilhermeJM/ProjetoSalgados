package br.com.salgados.dto.request;

import java.math.BigDecimal;

import br.com.salgados.domain.enums.Unit;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PurchaseItemRequest(
        @NotNull(message = "Selecione o ingrediente.")
        Long ingredientId,

        @NotNull(message = "Informe a quantidade.")
        @Positive(message = "A quantidade deve ser maior que zero.")
        BigDecimal quantity,

        @NotNull(message = "Informe a unidade.")
        Unit unit,

        /** Valor total pago por esta quantidade, em reais. */
        @NotNull(message = "Informe o valor total pago.")
        @Positive(message = "O valor total deve ser maior que zero.")
        @Digits(integer = 9, fraction = 2, message = "O valor total deve ter no máximo 2 casas decimais (centavos).")
        BigDecimal totalCost) {
}