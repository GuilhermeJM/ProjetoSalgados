package br.com.salgados.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record Purchase(
        Long id,
        LocalDate purchaseDate,
        String notes,
        BigDecimal totalAmount,
        LocalDateTime createdAt) {
}