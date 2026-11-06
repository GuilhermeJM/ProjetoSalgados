package br.com.salgados.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PurchaseRequest(
        @NotNull(message = "Informe a data da compra.")
        LocalDate purchaseDate,

        @Size(max = 500, message = "As observações podem ter no máximo 500 caracteres.")
        String notes,

        @NotNull(message = "Informe os itens da compra.")
        @Valid
        List<PurchaseItemRequest> items) {
}