package br.com.salgados.domain.model;

import br.com.salgados.domain.enums.Unit;

/** Movimento + dados do ingrediente (nome e unidade-base) vindos do JOIN, para exibir no extrato. */
public record StockMovementDetail(StockMovement movement, String ingredientName, Unit baseUnit) {
}