package br.com.salgados.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.com.salgados.domain.enums.AdjustmentDirection;
import br.com.salgados.domain.enums.MovementType;
import br.com.salgados.domain.enums.SourceType;
import br.com.salgados.domain.enums.Unit;
import br.com.salgados.dto.request.IngredientRequest;
import br.com.salgados.dto.request.PurchaseItemRequest;
import br.com.salgados.dto.request.PurchaseRequest;
import br.com.salgados.dto.request.StockAdjustmentRequest;
import br.com.salgados.dto.request.StockLossRequest;
import br.com.salgados.dto.response.IngredientResponse;
import br.com.salgados.dto.response.ReconciliationEntry;
import br.com.salgados.dto.response.ReconciliationResponse;
import br.com.salgados.dto.response.StockBalanceResponse;
import br.com.salgados.dto.response.StockMovementResponse;
import br.com.salgados.exception.BusinessException;
import br.com.salgados.repository.StockMovementRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StockServiceIntegrationTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private IngredientService ingredientService;

    @Autowired
    private StockMovementRepository movements;

    @Autowired
    private JdbcTemplate jdbc;

    // ---------- auxiliares ----------

    private long ingredient(String name, Unit base) {
        return ingredientService.create(new IngredientRequest(
                name, "Teste", base, false, BigDecimal.ZERO, null)).id();
    }

    private long ingredientWithMinimumKg(String name, String minimumKg) {
        return ingredientService.create(new IngredientRequest(
                name, "Teste", Unit.G, false, new BigDecimal(minimumKg), Unit.KG)).id();
    }

    private void buy(long ingredientId, LocalDate date, String quantity, Unit unit, String totalCost) {
        purchaseService.register(new PurchaseRequest(date, null, List.of(
                new PurchaseItemRequest(ingredientId, new BigDecimal(quantity), unit, new BigDecimal(totalCost)))));
    }

    private StockMovementResponse adjust(long ingredientId, AdjustmentDirection direction, String quantity,
                                         Unit unit, LocalDate date, String reason) {
        return stockService.adjust(new StockAdjustmentRequest(
                ingredientId, direction, new BigDecimal(quantity), unit, date, reason));
    }

    private StockMovementResponse loss(long ingredientId, String quantity, Unit unit,
                                       LocalDate date, String reason) {
        return stockService.registerLoss(new StockLossRequest(
                ingredientId, new BigDecimal(quantity), unit, date, reason));
    }

    private IngredientResponse ing(long id) {
        return ingredientService.findById(id);
    }

    private static void assertValor(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " mas foi " + real);
    }

    // ---------- ajuste ----------

    @Test
    void ajusteParaMaisAumentaOSaldoSemMudarOCustoMedio() {
        long farinha = ingredient("Farinha", Unit.G);
        buy(farinha, LocalDate.now(), "10", Unit.KG, "50");

        StockMovementResponse m = adjust(farinha, AdjustmentDirection.INCREASE, "2", Unit.KG,
                LocalDate.now(), "Contagem do estoque");

        assertEquals(MovementType.ADJUSTMENT, m.movementType());
        assertEquals(SourceType.MANUAL, m.sourceType());
        assertNull(m.sourceId());
        assertValor("2000", m.quantity());
        assertEquals("Contagem do estoque", m.reason());
        assertEquals("Farinha", m.ingredientName());
        assertValor("12000", ing(farinha).currentStock());
        assertValor("0.005", ing(farinha).currentAverageCost());
    }

    @Test
    void ajusteParaMenosReduzOSaldoEPermiteZerar() {
        long farinha = ingredient("Farinha", Unit.G);
        buy(farinha, LocalDate.now(), "10", Unit.KG, "50");

        StockMovementResponse m = adjust(farinha, AdjustmentDirection.DECREASE, "4", Unit.KG,
                LocalDate.now(), "Erro de lançamento");
        assertValor("-4000", m.quantity());
        assertValor("6000", ing(farinha).currentStock());

        adjust(farinha, AdjustmentDirection.DECREASE, "6", Unit.KG, LocalDate.now(), "Zerando o estoque");
        assertValor("0", ing(farinha).currentStock());
        assertValor("0", movements.sumQuantity(farinha));
        assertValor("0.005", ing(farinha).currentAverageCost());
    }

    @Test
    void t10_ajusteSemJustificativaERejeitado() {
        long farinha = ingredient("Farinha", Unit.G);
        buy(farinha, LocalDate.now(), "10", Unit.KG, "50");

        for (String reason : new String[] { null, "", "   " }) {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> adjust(farinha, AdjustmentDirection.INCREASE, "1", Unit.KG, LocalDate.now(), reason));
            assertEquals("REASON_REQUIRED", ex.getCode());
        }

        assertValor("10000", ing(farinha).currentStock());
        assertEquals(1, movements.findAllByIngredient(farinha).size());   // só a compra
    }

    // ---------- perda ----------

    @Test
    void perdaReduzOSaldoERegistraTipoLoss() {
        long farinha = ingredient("Farinha", Unit.G);
        buy(farinha, LocalDate.now(), "10", Unit.KG, "50");

        StockMovementResponse m = loss(farinha, "300", Unit.G, LocalDate.now(), "Caiu no chão");

        assertEquals(MovementType.LOSS, m.movementType());
        assertEquals(SourceType.MANUAL, m.sourceType());
        assertValor("-300", m.quantity());
        assertValor("9700", ing(farinha).currentStock());
    }

    @Test
    void perdaSemMotivoERejeitada() {
        long farinha = ingredient("Farinha", Unit.G);
        buy(farinha, LocalDate.now(), "10", Unit.KG, "50");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loss(farinha, "100", Unit.G, LocalDate.now(), "   "));
        assertEquals("REASON_REQUIRED", ex.getCode());
        assertValor("10000", ing(farinha).currentStock());
    }

    // ---------- saldo negativo ----------

    @Test
    void rejeitaSaidaMaiorQueOSaldoSemGravarNada() {
        long farinha = ingredient("Farinha", Unit.G);
        buy(farinha, LocalDate.now(), "1", Unit.KG, "5");

        BusinessException ajuste = assertThrows(BusinessException.class,
                () -> adjust(farinha, AdjustmentDirection.DECREASE, "2", Unit.KG, LocalDate.now(), "Teste"));
        assertEquals("INSUFFICIENT_STOCK", ajuste.getCode());

        BusinessException perda = assertThrows(BusinessException.class,
                () -> loss(farinha, "1500", Unit.G, LocalDate.now(), "Teste"));
        assertEquals("INSUFFICIENT_STOCK", perda.getCode());

        assertValor("1000", ing(farinha).currentStock());
        assertEquals(1, movements.findAllByIngredient(farinha).size());
    }

    @Test
    void saldoNegativoNoHistoricoERecusadoSemGravarNada() {
        long farinha = ingredient("Farinha", Unit.G);
        buy(farinha, LocalDate.now().minusDays(1), "10", Unit.KG, "50");

        // hoje há 10 kg, mas há 3 dias ainda não havia nada em estoque
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adjust(farinha, AdjustmentDirection.DECREASE, "5", Unit.KG,
                        LocalDate.now().minusDays(3), "Data errada"));
        assertEquals("NEGATIVE_STOCK_IN_HISTORY", ex.getCode());

        assertValor("10000", ing(farinha).currentStock());
        assertEquals(1, movements.findAllByIngredient(farinha).size());
    }

    @Test
    void ajusteRetroativoRecalculaOCustoMedio() {
        long farinha = ingredient("Farinha", Unit.G);
        buy(farinha, LocalDate.now().minusDays(3), "10", Unit.KG, "50");     // R$ 0,005/g
        buy(farinha, LocalDate.now().minusDays(1), "10", Unit.KG, "100");    // R$ 0,01/g
        assertValor("0.0075", ing(farinha).currentAverageCost());

        // a perda aconteceu entre as duas compras
        loss(farinha, "5000", Unit.G, LocalDate.now().minusDays(2), "Perda retroativa");

        // dia -3: 10000 g a 0,005 | dia -2: sobram 5000 g | dia -1: (5000 x 0,005 + 100) / 15000
        assertValor("15000", ing(farinha).currentStock());
        assertValor("0.0083333333", ing(farinha).currentAverageCost());
        assertTrue(stockService.reconcile(false).divergences().isEmpty());
    }

    // ---------- outras validações ----------

    @Test
    void rejeitaDataFutura() {
        long farinha = ingredient("Farinha", Unit.G);
        buy(farinha, LocalDate.now(), "10", Unit.KG, "50");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adjust(farinha, AdjustmentDirection.INCREASE, "1", Unit.KG,
                        LocalDate.now().plusDays(1), "Teste"));
        assertEquals("MOVEMENT_DATE_IN_FUTURE", ex.getCode());
    }

    @Test
    void bloqueiaConversaoDeGramaParaMililitro() {
        long leite = ingredient("Leite", Unit.ML);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loss(leite, "500", Unit.G, LocalDate.now(), "Teste"));
        assertEquals("INVALID_UNIT_CONVERSION", ex.getCode());
    }

    @Test
    void rejeitaIngredienteInexistente() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adjust(999999, AdjustmentDirection.INCREASE, "1", Unit.KG, LocalDate.now(), "Teste"));
        assertEquals("INGREDIENT_NOT_FOUND", ex.getCode());
    }

    @Test
    void permiteBaixaDeIngredienteInativo() {
        long farinha = ingredient("Farinha", Unit.G);
        buy(farinha, LocalDate.now(), "10", Unit.KG, "50");
        ingredientService.setActive(farinha, false);

        loss(farinha, "100", Unit.G, LocalDate.now(), "Descarte");

        assertValor("9900", ing(farinha).currentStock());
    }

    // ---------- extrato ----------

    @Test
    void extratoOrdenaDoMaisRecenteParaOMaisAntigo() {
        long farinha = ingredient("Farinha", Unit.G);
        buy(farinha, LocalDate.now(), "10", Unit.KG, "50");
        adjust(farinha, AdjustmentDirection.INCREASE, "1", Unit.KG, LocalDate.now(), "Contagem");
        loss(farinha, "200", Unit.G, LocalDate.now(), "Caiu");

        List<StockMovementResponse> list = stockService.listMovements(farinha, null, null, null, null);

        assertEquals(3, list.size());
        assertEquals(MovementType.LOSS, list.get(0).movementType());
        assertEquals(MovementType.ADJUSTMENT, list.get(1).movementType());
        assertEquals(MovementType.PURCHASE, list.get(2).movementType());
        assertEquals("Farinha", list.get(0).ingredientName());
        assertEquals(Unit.G, list.get(0).baseUnit());
    }

    @Test
    void extratoFiltraPorTipoEIngrediente() {
        long farinha = ingredient("Farinha", Unit.G);
        long leite = ingredient("Leite", Unit.ML);
        buy(farinha, LocalDate.now(), "10", Unit.KG, "50");
        buy(leite, LocalDate.now(), "1", Unit.L, "5");
        loss(farinha, "100", Unit.G, LocalDate.now(), "Caiu");
        loss(leite, "100", Unit.ML, LocalDate.now(), "Derramou");

        assertEquals(2, stockService.listMovements(null, MovementType.LOSS, null, null, null).size());
        assertEquals(1, stockService.listMovements(leite, MovementType.LOSS, null, null, null).size());
        assertEquals(1, stockService.listMovements(farinha, MovementType.PURCHASE, null, null, null).size());
    }

    @Test
    void extratoFiltraPorPeriodo() {
        long farinha = ingredient("Farinha", Unit.G);
        buy(farinha, LocalDate.now().minusDays(10), "10", Unit.KG, "50");
        adjust(farinha, AdjustmentDirection.INCREASE, "1", Unit.KG, LocalDate.now(), "Contagem");

        List<StockMovementResponse> recentes = stockService.listMovements(
                farinha, null, LocalDate.now().minusDays(3), null, null);
        assertEquals(1, recentes.size());
        assertEquals(MovementType.ADJUSTMENT, recentes.get(0).movementType());

        List<StockMovementResponse> antigos = stockService.listMovements(
                farinha, null, null, LocalDate.now().minusDays(5), null);
        assertEquals(1, antigos.size());
        assertEquals(MovementType.PURCHASE, antigos.get(0).movementType());

        // os limites do período são inclusivos
        assertEquals(1, stockService.listMovements(farinha, null,
                LocalDate.now().minusDays(10), LocalDate.now().minusDays(10), null).size());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> stockService.listMovements(farinha, null,
                        LocalDate.now(), LocalDate.now().minusDays(5), null));
        assertEquals("INVALID_PERIOD", ex.getCode());
    }

    @Test
    void extratoRespeitaOLimite() {
        long farinha = ingredient("Farinha", Unit.G);
        buy(farinha, LocalDate.now(), "10", Unit.KG, "50");
        adjust(farinha, AdjustmentDirection.INCREASE, "1", Unit.KG, LocalDate.now(), "Contagem");
        loss(farinha, "200", Unit.G, LocalDate.now(), "Caiu");

        List<StockMovementResponse> dois = stockService.listMovements(farinha, null, null, null, 2);
        assertEquals(2, dois.size());
        assertEquals(MovementType.LOSS, dois.get(0).movementType());

        // limite inválido (zero) vira 1
        assertEquals(1, stockService.listMovements(farinha, null, null, null, 0).size());
    }

    // ---------- saldo ----------

    @Test
    void saldoMostraValorDoEstoqueEAlertaDeMinimo() {
        long farinha = ingredientWithMinimumKg("Farinha", "5");

        buy(farinha, LocalDate.now(), "3", Unit.KG, "15");
        StockBalanceResponse abaixo = stockService.balance("Farinha", null, null, null).get(0);
        assertValor("3000", abaixo.currentStock());
        assertValor("5000", abaixo.minimumStock());
        assertValor("0.005", abaixo.averageCost());
        assertValor("15", abaixo.stockValue());
        assertTrue(abaixo.belowMinimum());

        buy(farinha, LocalDate.now(), "4", Unit.KG, "20");
        StockBalanceResponse acima = stockService.balance("Farinha", null, null, null).get(0);
        assertValor("7000", acima.currentStock());
        assertValor("35", acima.stockValue());
        assertFalse(acima.belowMinimum());
    }

    @Test
    void filtraSomenteOsAbaixoDoMinimo() {
        long farinha = ingredientWithMinimumKg("Farinha", "5");
        long sal = ingredientWithMinimumKg("Sal", "1");
        buy(farinha, LocalDate.now(), "3", Unit.KG, "15");   // abaixo do mínimo
        buy(sal, LocalDate.now(), "2", Unit.KG, "4");        // acima do mínimo

        List<StockBalanceResponse> abaixo = stockService.balance(null, null, null, true);
        assertEquals(1, abaixo.size());
        assertEquals("Farinha", abaixo.get(0).name());

        assertEquals(2, stockService.balance(null, null, null, null).size());
    }

    // ---------- reconciliação ----------

    @Test
    void reconciliacaoSemDivergenciasDepoisDasOperacoes() {
        long farinha = ingredient("Farinha", Unit.G);
        long leite = ingredient("Leite", Unit.ML);
        buy(farinha, LocalDate.now(), "10", Unit.KG, "50");
        buy(farinha, LocalDate.now(), "5", Unit.KG, "30");
        buy(leite, LocalDate.now(), "2", Unit.L, "9");
        adjust(farinha, AdjustmentDirection.DECREASE, "1", Unit.KG, LocalDate.now(), "Contagem");
        loss(leite, "100", Unit.ML, LocalDate.now(), "Derramou");

        ReconciliationResponse r = stockService.reconcile(false);

        assertTrue(r.checked() >= 2);
        assertFalse(r.fixed());
        assertTrue(r.divergences().isEmpty());
    }

    @Test
    void reconciliacaoDetectaECorrigeDivergencia() {
        long farinha = ingredient("Farinha", Unit.G);
        buy(farinha, LocalDate.now(), "10", Unit.KG, "50");
        jdbc.update("UPDATE ingredient SET current_stock = '999' WHERE id = ?", farinha);

        ReconciliationResponse check = stockService.reconcile(false);
        ReconciliationEntry entry = check.divergences().stream()
                .filter(d -> d.ingredientId().equals(farinha)).findFirst().orElseThrow();
        assertValor("999", entry.storedStock());
        assertValor("10000", entry.expectedStock());
        assertValor("999", ing(farinha).currentStock());        // conferir não altera nada

        ReconciliationResponse fix = stockService.reconcile(true);
        assertTrue(fix.fixed());
        assertTrue(fix.divergences().stream().anyMatch(d -> d.ingredientId().equals(farinha)));
        assertValor("10000", ing(farinha).currentStock());

        assertTrue(stockService.reconcile(false).divergences().stream()
                .noneMatch(d -> d.ingredientId().equals(farinha)));
    }
}