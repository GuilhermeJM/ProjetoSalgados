package br.com.salgados.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.salgados.domain.enums.MovementType;
import br.com.salgados.domain.enums.SourceType;

/** quantity é assinada, na unidade-base: positiva = entrada, negativa = saída. */
public record StockMovement(
        Long id,
        Long ingredientId,
        MovementType movementType,
        BigDecimal quantity,
        SourceType sourceType,
        Long sourceId,
        LocalDateTime occurredAt,
        String reason,
        LocalDateTime createdAt) {
}