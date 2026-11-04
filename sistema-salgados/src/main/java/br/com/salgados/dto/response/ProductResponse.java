package br.com.salgados.dto.response;

import java.math.BigDecimal;

import br.com.salgados.domain.model.Product;

public record ProductResponse(
        Long id,
        String name,
        String category,
        String description,
        BigDecimal unitWeight,
        boolean active) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(p.id(), p.name(), p.category(), p.description(),
                p.unitWeight(), p.active());
    }
}