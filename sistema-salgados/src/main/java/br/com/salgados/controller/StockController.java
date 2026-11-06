package br.com.salgados.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.salgados.domain.enums.MovementType;
import br.com.salgados.domain.service.StockService;
import br.com.salgados.dto.request.StockAdjustmentRequest;
import br.com.salgados.dto.request.StockLossRequest;
import br.com.salgados.dto.response.ReconciliationResponse;
import br.com.salgados.dto.response.StockBalanceResponse;
import br.com.salgados.dto.response.StockMovementResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockService service;

    public StockController(StockService service) {
        this.service = service;
    }

    @GetMapping("/balance")
    public List<StockBalanceResponse> balance(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean belowMinimum) {
        return service.balance(search, category, active, belowMinimum);
    }

    @GetMapping("/movements")
    public List<StockMovementResponse> movements(
            @RequestParam(required = false) Long ingredientId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer limit) {
        return service.listMovements(ingredientId, type, from, to, limit);
    }

    @PostMapping("/adjustments")
    public ResponseEntity<StockMovementResponse> adjust(@Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.adjust(request));
    }

    @PostMapping("/losses")
    public ResponseEntity<StockMovementResponse> registerLoss(@Valid @RequestBody StockLossRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registerLoss(request));
    }

    /** Só confere e informa; não altera nada. */
    @GetMapping("/reconciliation")
    public ReconciliationResponse checkReconciliation() {
        return service.reconcile(false);
    }

    /** Corrige os ingredientes divergentes com os valores calculados a partir do extrato. */
    @PostMapping("/reconciliation")
    public ReconciliationResponse fixReconciliation() {
        return service.reconcile(true);
    }
}