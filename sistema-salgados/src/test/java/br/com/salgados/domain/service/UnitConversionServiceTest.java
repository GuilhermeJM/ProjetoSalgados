package br.com.salgados.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.salgados.domain.enums.Unit;
import br.com.salgados.exception.BusinessException;

class UnitConversionServiceTest {

    private final UnitConversionService service = new UnitConversionService();

    // BigDecimal.equals() considera a escala (2500 != 2500.0), por isso comparamos com compareTo.
    private static void assertValor(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real),
                "esperado " + esperado + " mas foi " + real);
    }

    @Test
    void converteQuilosParaGramas() {
        assertValor("2500", service.toBase(new BigDecimal("2.5"), Unit.KG, Unit.G));
    }

    @Test
    void converteLitrosParaMililitros() {
        assertValor("1500", service.toBase(new BigDecimal("1.5"), Unit.L, Unit.ML));
    }

    @Test
    void mantemValorQuandoJaEstaNaUnidadeBase() {
        assertValor("300", service.toBase(new BigDecimal("300"), Unit.G, Unit.G));
    }

    @Test
    void converteDaBaseParaExibicao() {
        assertValor("4", service.fromBase(new BigDecimal("4000"), Unit.KG));
    }

    @Test
    void bloqueiaGramaParaMililitro() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.toBase(BigDecimal.ONE, Unit.G, Unit.ML));
        assertEquals("INVALID_UNIT_CONVERSION", ex.getCode());
    }

    @Test
    void bloqueiaQuiloParaUnidade() {
        assertThrows(BusinessException.class,
                () -> service.toBase(BigDecimal.ONE, Unit.KG, Unit.UN));
    }

    @Test
    void rejeitaQuantidadeZeroOuNegativa() {
        assertThrows(BusinessException.class,
                () -> service.validateQuantity(BigDecimal.ZERO, Unit.G, false));
        assertThrows(BusinessException.class,
                () -> service.validateQuantity(new BigDecimal("-1"), Unit.G, false));
    }

    @Test
    void rejeitaFracaoEmUnidadesInteiras() {
        assertThrows(BusinessException.class,
                () -> service.validateQuantity(new BigDecimal("1.5"), Unit.UN, false));
    }

    @Test
    void aceitaFracaoQuandoHabilitada() {
        assertDoesNotThrow(() -> service.validateQuantity(new BigDecimal("1.5"), Unit.UN, true));
    }

    @Test
    void aceitaInteiroEscritoComCasasDecimais() {
        assertDoesNotThrow(() -> service.validateQuantity(new BigDecimal("3.0"), Unit.UN, false));
    }
}