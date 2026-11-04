package br.com.salgados.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.salgados.domain.service.RecipeService;
import br.com.salgados.dto.request.RecipeRequest;
import br.com.salgados.dto.response.RecipeResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class RecipeController {

    private final RecipeService service;

    public RecipeController(RecipeService service) {
        this.service = service;
    }

    @GetMapping("/products/{productId}/recipes")
    public List<RecipeResponse> listByProduct(@PathVariable long productId) {
        return service.listByProduct(productId);
    }

    @PostMapping("/products/{productId}/recipes")
    public ResponseEntity<RecipeResponse> create(@PathVariable long productId,
                                                 @Valid @RequestBody RecipeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(productId, request));
    }

    @GetMapping("/recipes/{id}")
    public RecipeResponse get(@PathVariable long id) {
        return service.findById(id);
    }
}