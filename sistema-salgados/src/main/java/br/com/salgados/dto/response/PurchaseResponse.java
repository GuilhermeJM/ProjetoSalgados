package br.com.salgados.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseResponse(
        Long id,
        LocalDate purchaseDate,
        String notes,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        List<PurchaseItemResponse> items) {
}