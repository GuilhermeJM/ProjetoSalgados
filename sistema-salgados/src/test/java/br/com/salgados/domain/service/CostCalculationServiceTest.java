package br.com.salgados.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CostCalculationServiceTest {

    private final CostCalculationService service = new CostCalculationService();

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static void assertValor(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " mas foi " + real);
    }

    @Test
    void custoUnitarioDeDezQuilosPorCinquentaReais() {
        // T-01: 10 kg (10000 g) por R$ 50 -> R$ 0,005/g = R$ 5/kg
        assertValor("0.005", service.unitCost(bd("50"), bd("10000")));
    }

    @Test
    void primeiraCompraUsaOCustoDaPropriaCompra() {
        assertValor("0.005", service.newAverageCost(bd("0"), bd("0"), bd("10000"), bd("50")));
    }

    @Test
    void t02_segundaCompraGeraCustoMedioPonderado() {
        // saldo 10 kg a R$ 5/kg; compra 5 kg por R$ 30 -> 15 kg a R$ 5,333333/kg
        BigDecimal media = service.newAverageCost(bd("10000"), bd("0.005"), bd("5000"), bd("30"));

        assertValor("0.0053333333", media);
        assertValor("5.3333333", media.multiply(bd("1000")));
    }

    @Test
    void saldoZeradoIgnoraOCustoAntigo() {
        // sobrou custo médio de uma compra anterior, mas o saldo acabou
        assertValor("0.006", service.newAverageCost(bd("0"), bd("9.99"), bd("1000"), bd("6")));
    }

    @Test
    void arredondaMeioParaCimaComDezCasas() {
        assertValor("0.6666666667", service.unitCost(bd("2"), bd("3")));
        assertValor("3.3333333333", service.unitCost(bd("10"), bd("3")));
    }

    @Test
    void mediasSucessivasNaoAcumulamErroDeArredondamento() {
        // 3 compras de 1 kg: R$ 3,33 + R$ 3,34 + R$ 3,33 = R$ 10,00 por 3000 g
        BigDecimal m1 = service.newAverageCost(bd("0"), bd("0"), bd("1000"), bd("3.33"));
        BigDecimal m2 = service.newAverageCost(bd("1000"), m1, bd("1000"), bd("3.34"));
        BigDecimal m3 = service.newAverageCost(bd("2000"), m2, bd("1000"), bd("3.33"));

        assertValor("0.0033333333", m3);   // = 10 / 3000, arredondado
    }

    @Test
    void rejeitaQuantidadeZero() {
        assertThrows(IllegalArgumentException.class, () -> service.unitCost(bd("10"), bd("0")));
    }
}