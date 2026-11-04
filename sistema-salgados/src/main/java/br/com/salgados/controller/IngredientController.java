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

import br.com.salgados.domain.service.IngredientService;
import br.com.salgados.dto.request.IngredientRequest;
import br.com.salgados.dto.request.IngredientStatusRequest;
import br.com.salgados.dto.response.IngredientResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ingredients")
public class IngredientController {

    private final IngredientService service;

    public IngredientController(IngredientService service) {
        this.service = service;
    }

    @GetMapping
    public List<IngredientResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean active) {
        return service.list(search, category, active);
    }

    @GetMapping("/{id}")
    public IngredientResponse get(@PathVariable long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<IngredientResponse> create(@Valid @RequestBody IngredientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public IngredientResponse update(@PathVariable long id, @Valid @RequestBody IngredientRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public IngredientResponse setStatus(@PathVariable long id,
                                        @Valid @RequestBody IngredientStatusRequest request) {
        return service.setActive(id, request.active());
    }
}