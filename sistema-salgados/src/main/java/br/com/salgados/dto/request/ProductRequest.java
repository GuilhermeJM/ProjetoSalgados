package br.com.salgados.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank(message = "Informe o nome do produto.")
        @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres.")
        String name,

        @Size(max = 50, message = "A categoria pode ter no máximo 50 caracteres.")
        String category,

        @Size(max = 500, message = "A descrição pode ter no máximo 500 caracteres.")
        String description,

        /** Peso final de cada unidade, em gramas. Opcional. */
        @Positive(message = "O peso unitário deve ser maior que zero.")
        BigDecimal unitWeight) {
}