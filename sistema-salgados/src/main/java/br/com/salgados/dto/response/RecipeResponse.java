package br.com.salgados.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.com.salgados.domain.enums.RecipeStatus;

public record RecipeResponse(
        Long id,
        Long productId,
        String productName,
        int version,
        BigDecimal yieldQuantity,
        String yieldUnit,
        RecipeStatus status,
        LocalDateTime createdAt,
        List<RecipeItemResponse> items) {
}