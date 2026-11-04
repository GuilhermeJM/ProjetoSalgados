package br.com.salgados.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** unitWeight é o peso final de cada unidade, em gramas (opcional). */
public record Product(
        Long id,
        String name,
        String category,
        String description,
        BigDecimal unitWeight,
        boolean active,
        LocalDateTime createdAt) {
}