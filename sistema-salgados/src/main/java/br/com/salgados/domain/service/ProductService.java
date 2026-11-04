package br.com.salgados.domain.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.salgados.domain.model.Product;
import br.com.salgados.dto.request.ProductRequest;
import br.com.salgados.dto.response.ProductResponse;
import br.com.salgados.exception.ConflictException;
import br.com.salgados.exception.NotFoundException;
import br.com.salgados.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<ProductResponse> list(String search, String category, Boolean active) {
        return repository.search(search, category, active).stream()
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse findById(long id) {
        return ProductResponse.from(get(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String name = request.name().trim();
        checkNameAvailable(name, null);

        long id = repository.insert(new Product(null, name, normalize(request.category()),
                normalize(request.description()), request.unitWeight(), true, LocalDateTime.now()));
        return findById(id);
    }

    @Transactional
    public ProductResponse update(long id, ProductRequest request) {
        Product current = get(id);
        String name = request.name().trim();
        checkNameAvailable(name, id);

        repository.update(new Product(id, name, normalize(request.category()),
                normalize(request.description()), request.unitWeight(), current.active(),
                current.createdAt()));
        return findById(id);
    }

    @Transactional
    public ProductResponse setActive(long id, boolean active) {
        get(id);
        repository.setActive(id, active);
        return findById(id);
    }

    // ---------- auxiliares ----------

    private Product get(long id) {
        return repository.findById(id).orElseThrow(() ->
                new NotFoundException("PRODUCT_NOT_FOUND", "Produto não encontrado."));
    }

    private void checkNameAvailable(String name, Long excludeId) {
        if (repository.existsByName(name, excludeId)) {
            throw new ConflictException("DUPLICATE_PRODUCT_NAME",
                    "Já existe um produto chamado \"" + name + "\".");
        }
    }

    private static String normalize(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}