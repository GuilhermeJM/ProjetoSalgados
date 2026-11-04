package br.com.salgados.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.com.salgados.domain.enums.Unit;
import br.com.salgados.dto.request.IngredientRequest;
import br.com.salgados.dto.response.IngredientResponse;
import br.com.salgados.exception.BusinessException;
import br.com.salgados.exception.ConflictException;
import br.com.salgados.exception.NotFoundException;

// @Transactional no teste: cada método roda numa transação que é desfeita no final,
// então um teste não enxerga os dados do outro.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IngredientServiceIntegrationTest {

    @Autowired
    private IngredientService service;

    @Autowired
    private JdbcTemplate jdbc;

    private static IngredientRequest request(String name, Unit base, String minimum, Unit minimumUnit) {
        return new IngredientRequest(name, "Secos", base, false, new BigDecimal(minimum), minimumUnit);
    }

    @Test
    void criaIngredienteConvertendoEstoqueMinimoParaUnidadeBase() {
        IngredientResponse r = service.create(request("Farinha de trigo", Unit.G, "5", Unit.KG));

        assertEquals(0, new BigDecimal("5000").compareTo(r.minimumStock()));
        assertEquals(0, BigDecimal.ZERO.compareTo(r.currentStock()));
        assertTrue(r.active());
        assertFalse(r.belowMinimum());
    }

    @Test
    void preservaPrecisaoDecimalNoBanco() {
        IngredientResponse r = service.create(request("Fermento", Unit.G, "0.333", null));

        assertEquals(0, new BigDecimal("0.333").compareTo(service.findById(r.id()).minimumStock()));
    }

    @Test
    void rejeitaNomeDuplicadoIgnorandoMaiusculas() {
        service.create(request("Farinha", Unit.G, "0", null));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.create(request("FARINHA", Unit.G, "0", null)));
        assertEquals("DUPLICATE_INGREDIENT_NAME", ex.getCode());
    }

    @Test
    void rejeitaUnidadeBaseQueNaoEBase() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(request("Açúcar", Unit.KG, "0", null)));
        assertEquals("INVALID_BASE_UNIT", ex.getCode());
    }

    @Test
    void rejeitaEstoqueMinimoEmGrandezaDiferente() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(request("Leite", Unit.G, "1", Unit.ML)));
        assertEquals("INVALID_UNIT_CONVERSION", ex.getCode());
    }

    @Test
    void inativaIngredienteEFiltraPorStatus() {
        IngredientResponse r = service.create(request("Óleo", Unit.ML, "0", null));
        service.setActive(r.id(), false);

        assertTrue(service.list(null, null, true).stream().noneMatch(i -> i.id().equals(r.id())));
        assertTrue(service.list(null, null, false).stream().anyMatch(i -> i.id().equals(r.id())));
    }

    @Test
    void atualizarInexistenteLancaNotFound() {
        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.update(9999, request("Qualquer", Unit.G, "0", null)));
        assertEquals("INGREDIENT_NOT_FOUND", ex.getCode());
    }

    @Test
    void bloqueiaTrocaDeUnidadeQuandoJaExisteHistorico() {
        IngredientResponse r = service.create(request("Sal", Unit.G, "0", null));
        jdbc.update("""
                INSERT INTO stock_movement
                    (ingredient_id, movement_type, quantity, source_type, occurred_at, created_at)
                VALUES (?, 'ADJUSTMENT', '100', 'MANUAL', '2026-11-03T10:00:00', '2026-11-03T10:00:00')
                """, r.id());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.update(r.id(), request("Sal", Unit.ML, "0", null)));
        assertEquals("UNIT_LOCKED", ex.getCode());
    }
}