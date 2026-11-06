package br.com.salgados.domain.model;

import java.math.BigDecimal;

public record PurchaseLineCost(long purchaseId, BigDecimal quantityBase, BigDecimal totalCost) {
}