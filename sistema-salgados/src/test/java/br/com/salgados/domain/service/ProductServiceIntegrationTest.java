package br.com.salgados.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.com.salgados.dto.request.ProductRequest;
import br.com.salgados.dto.response.ProductResponse;
import br.com.salgados.exception.ConflictException;
import br.com.salgados.exception.NotFoundException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService service;

    private static ProductRequest request(String name, String weight) {
        return new ProductRequest(name, "Fritos", "Descrição de teste",
                weight == null ? null : new BigDecimal(weight));
    }

    @Test
    void criaProdutoAtivoComPeso() {
        ProductResponse r = service.create(request("Coxinha de frango", "45.5"));

        assertTrue(r.active());
        assertEquals("Coxinha de frango", r.name());
        assertEquals(0, new BigDecimal("45.5").compareTo(r.unitWeight()));
    }

    @Test
    void pesoUnitarioEOpcional() {
        ProductResponse r = service.create(request("Empada", null));

        assertNull(service.findById(r.id()).unitWeight());
    }

    @Test
    void rejeitaNomeDuplicadoIgnorandoMaiusculas() {
        service.create(request("Esfiha", null));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.create(request("ESFIHA", null)));
        assertEquals("DUPLICATE_PRODUCT_NAME", ex.getCode());
    }

    @Test
    void atualizaCadastroSemAlterarStatus() {
        ProductResponse r = service.create(request("Bolinha de queijo", "30"));
        service.setActive(r.id(), false);

        ProductResponse updated = service.update(r.id(), request("Bolinha de queijo grande", "60"));

        assertEquals("Bolinha de queijo grande", updated.name());
        assertEquals(0, new BigDecimal("60").compareTo(updated.unitWeight()));
        assertEquals(false, updated.active());
    }

    @Test
    void permiteManterOProprioNomeAoAtualizar() {
        ProductResponse r = service.create(request("Kibe", null));

        // não pode acusar duplicidade contra si mesmo
        ProductResponse updated = service.update(r.id(), request("Kibe", "50"));
        assertEquals("Kibe", updated.name());
    }

    @Test
    void inativaProdutoEFiltraPorStatus() {
        ProductResponse r = service.create(request("Risole", null));
        service.setActive(r.id(), false);

        assertTrue(service.list(null, null, true).stream().noneMatch(p -> p.id().equals(r.id())));
        assertTrue(service.list(null, null, false).stream().anyMatch(p -> p.id().equals(r.id())));
    }

    @Test
    void atualizarInexistenteLancaNotFound() {
        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.update(9999, request("Qualquer", null)));
        assertEquals("PRODUCT_NOT_FOUND", ex.getCode());
    }
}