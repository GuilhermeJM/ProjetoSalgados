package br.com.salgados.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.salgados.domain.service.ProductService;
import br.com.salgados.dto.request.ProductRequest;
import br.com.salgados.dto.request.ProductStatusRequest;
import br.com.salgados.dto.response.ProductResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean active) {
        return service.list(search, category, active);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable long id, @Valid @RequestBody ProductRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public ProductResponse setStatus(@PathVariable long id,
                                     @Valid @RequestBody ProductStatusRequest request) {
        return service.setActive(id, request.active());
    }
}