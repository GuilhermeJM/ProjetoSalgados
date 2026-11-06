package br.com.salgados.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.salgados.domain.enums.MovementType;
import br.com.salgados.domain.enums.SourceType;
import br.com.salgados.domain.enums.Unit;
import br.com.salgados.domain.model.StockMovement;
import br.com.salgados.domain.model.StockMovementDetail;

/** quantity é assinada e está na unidade-base do ingrediente: positiva = entrada, negativa = saída. */
public record StockMovementResponse(
        Long id,
        Long ingredientId,
        String ingredientName,
        Unit baseUnit,
        MovementType movementType,
        BigDecimal quantity,
        SourceType sourceType,
        Long sourceId,
        LocalDateTime occurredAt,
        String reason,
        LocalDateTime createdAt) {

    public static StockMovementResponse from(StockMovementDetail detail) {
        StockMovement m = detail.movement();
        return new StockMovementResponse(m.id(), m.ingredientId(), detail.ingredientName(),
                detail.baseUnit(), m.movementType(), m.quantity(), m.sourceType(), m.sourceId(),
                m.occurredAt(), m.reason(), m.createdAt());
    }
}