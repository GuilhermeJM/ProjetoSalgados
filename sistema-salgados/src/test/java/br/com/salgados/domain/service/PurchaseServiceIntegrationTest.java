package br.com.salgados.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.com.salgados.domain.enums.MovementType;
import br.com.salgados.domain.enums.SourceType;
import br.com.salgados.domain.enums.Unit;
import br.com.salgados.domain.model.StockMovement;
import br.com.salgados.dto.request.IngredientRequest;
import br.com.salgados.dto.request.PurchaseItemRequest;
import br.com.salgados.dto.request.PurchaseRequest;
import br.com.salgados.dto.response.IngredientResponse;
import br.com.salgados.dto.response.PurchaseResponse;
import br.com.salgados.exception.BusinessException;
import br.com.salgados.exception.NotFoundException;
import br.com.salgados.repository.StockMovementRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PurchaseServiceIntegrationTest {

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private IngredientService ingredientService;

    @Autowired
    private StockMovementRepository movements;

    // ---------- auxiliares ----------

    private long ingredient(String name, Unit base, boolean allowFraction) {
        return ingredientService.create(new IngredientRequest(
                name, "Teste", base, allowFraction, BigDecimal.ZERO, null)).id();
    }

    private static PurchaseItemRequest line(long ingredientId, String quantity, Unit unit, String totalCost) {
        return new PurchaseItemRequest(ingredientId, new BigDecimal(quantity), unit, new BigDecimal(totalCost));
    }

    private PurchaseResponse buy(long ingredientId, String quantity, Unit unit, String totalCost) {
        return purchaseService.register(new PurchaseRequest(LocalDate.now(), null,
                List.of(line(ingredientId, quantity, unit, totalCost))));
    }

    private static void assertValor(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " mas foi " + real);
    }

    // ---------- custo médio (T-01 e T-02) ----------

    @Test
    void t01_compraInicialDeDezQuilosPorCinquentaReais() {
        long farinha = ingredient("Farinha", Unit.G, false);

        PurchaseResponse p = buy(farinha, "10", Unit.KG, "50");

        IngredientResponse i = ingredientService.findById(farinha);
        assertValor("10000", i.currentStock());
        assertValor("0.005", i.currentAverageCost());
        assertValor("5", i.currentAverageCost().multiply(new BigDecimal("1000")));   // R$ 5/kg
        assertValor("50", p.totalAmount());
        assertValor("0.005", p.items().get(0).unitCostBase());
    }

    @Test
    void t02_segundaCompraGeraCustoMedioPonderado() {
        long farinha = ingredient("Farinha", Unit.G, false);

        buy(farinha, "10", Unit.KG, "50");
        buy(farinha, "5", Unit.KG, "30");

        IngredientResponse i = ingredientService.findById(farinha);
        assertValor("15000", i.currentStock());
        assertValor("0.0053333333", i.currentAverageCost());
        assertValor("5.3333333", i.currentAverageCost().multiply(new BigDecimal("1000")));
    }

    @Test
    void compraAntigaMantemSeuCustoOriginalDepoisDeNovaCompra() {
        long farinha = ingredient("Farinha", Unit.G, false);

        PurchaseResponse first = buy(farinha, "10", Unit.KG, "50");
        buy(farinha, "5", Unit.KG, "30");

        PurchaseResponse reloaded = purchaseService.findById(first.id());
        assertValor("0.005", reloaded.items().get(0).unitCostBase());
        assertValor("50", reloaded.totalAmount());
    }

    // ---------- registro da compra ----------

    @Test
    void compraComVariosItensSomaOTotal() {
        long farinha = ingredient("Farinha", Unit.G, false);
        long leite = ingredient("Leite", Unit.ML, false);

        PurchaseResponse p = purchaseService.register(new PurchaseRequest(LocalDate.now(), "Mercado do bairro",
                List.of(line(farinha, "2", Unit.KG, "12.50"), line(leite, "3", Unit.L, "18.90"))));

        assertValor("31.40", p.totalAmount());
        assertEquals("Mercado do bairro", p.notes());
        assertEquals(2, p.items().size());
        assertEquals("Farinha", p.items().get(0).ingredientName());
        assertValor("2000", p.items().get(0).quantityBase());
        assertValor("3000", p.items().get(1).quantityBase());
    }

    @Test
    void converteLitrosParaMililitrosECalculaOCustoUnitario() {
        long leite = ingredient("Leite", Unit.ML, false);

        PurchaseResponse p = buy(leite, "3", Unit.L, "18.90");

        assertValor("3000", p.items().get(0).quantityBase());
        assertValor("0.0063", p.items().get(0).unitCostBase());
        assertValor("0.0063", ingredientService.findById(leite).currentAverageCost());
    }

    @Test
    void mesmoIngredienteEmDuasLinhasAcumulaEmSequencia() {
        long farinha = ingredient("Farinha", Unit.G, false);

        PurchaseResponse p = purchaseService.register(new PurchaseRequest(LocalDate.now(), null,
                List.of(line(farinha, "1", Unit.KG, "5"), line(farinha, "1", Unit.KG, "7"))));

        IngredientResponse i = ingredientService.findById(farinha);
        assertValor("12", p.totalAmount());
        assertValor("2000", i.currentStock());
        assertValor("0.006", i.currentAverageCost());    // (1000 x 0,005 + 7) / 2000
        assertEquals(2, movements.findBySource(SourceType.PURCHASE, p.id()).size());
    }

    // ---------- movimentos e saldo ----------

    @Test
    void geraMovimentoDeEntradaVinculadoACompra() {
        long farinha = ingredient("Farinha", Unit.G, false);

        PurchaseResponse p = buy(farinha, "10", Unit.KG, "50");

        List<StockMovement> list = movements.findBySource(SourceType.PURCHASE, p.id());
        assertEquals(1, list.size());
        StockMovement m = list.get(0);
        assertEquals(MovementType.PURCHASE, m.movementType());
        assertEquals(farinha, m.ingredientId().longValue());
        assertEquals(p.id(), m.sourceId());
        assertValor("10000", m.quantity());
        assertEquals(LocalDate.now(), m.occurredAt().toLocalDate());
    }

    @Test
    void saldoDoIngredienteBateComASomaDosMovimentos() {
        long farinha = ingredient("Farinha", Unit.G, false);
        long leite = ingredient("Leite", Unit.ML, false);

        buy(farinha, "10", Unit.KG, "50");
        buy(farinha, "5", Unit.KG, "30");
        buy(leite, "2", Unit.L, "9");

        assertEquals(0, ingredientService.findById(farinha).currentStock()
                .compareTo(movements.sumQuantity(farinha)));
        assertEquals(0, ingredientService.findById(leite).currentStock()
                .compareTo(movements.sumQuantity(leite)));
        assertValor("15000", movements.sumQuantity(farinha));
    }

    // ---------- validações ----------

    @Test
    void rejeitaCompraSemItens() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> purchaseService.register(new PurchaseRequest(LocalDate.now(), null, List.of())));
        assertEquals("PURCHASE_WITHOUT_ITEMS", ex.getCode());
    }

    @Test
    void rejeitaDataFutura() {
        long farinha = ingredient("Farinha", Unit.G, false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> purchaseService.register(new PurchaseRequest(LocalDate.now().plusDays(1), null,
                        List.of(line(farinha, "1", Unit.KG, "5")))));
        assertEquals("PURCHASE_DATE_IN_FUTURE", ex.getCode());
    }

    @Test
    void rejeitaIngredienteInativo() {
        long farinha = ingredient("Farinha", Unit.G, false);
        ingredientService.setActive(farinha, false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> buy(farinha, "1", Unit.KG, "5"));
        assertEquals("INGREDIENT_INACTIVE", ex.getCode());
    }

    @Test
    void rejeitaIngredienteInexistente() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> buy(999999, "1", Unit.KG, "5"));
        assertEquals("INGREDIENT_NOT_FOUND", ex.getCode());
    }

    @Test
    void bloqueiaConversaoDeGramaParaMililitro() {
        long leite = ingredient("Leite", Unit.ML, false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> buy(leite, "500", Unit.G, "5"));
        assertEquals("INVALID_UNIT_CONVERSION", ex.getCode());
    }

    @Test
    void rejeitaFracaoEmIngredienteContadoEmUnidadesInteiras() {
        long ovo = ingredient("Ovo", Unit.UN, false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> buy(ovo, "1.5", Unit.UN, "5"));
        assertEquals("INVALID_QUANTITY", ex.getCode());
    }

    @Test
    void linhaInvalidaNaoAlteraOsOutrosIngredientes() {
        long farinha = ingredient("Farinha", Unit.G, false);
        long leite = ingredient("Leite", Unit.ML, false);

        // a primeira linha é válida; a segunda tenta converter g em ml
        assertThrows(BusinessException.class,
                () -> purchaseService.register(new PurchaseRequest(LocalDate.now(), null,
                        List.of(line(farinha, "1", Unit.KG, "5"), line(leite, "500", Unit.G, "5")))));

        assertValor("0", ingredientService.findById(farinha).currentStock());
        assertValor("0", movements.sumQuantity(farinha));
    }

    // ---------- consultas ----------

    @Test
    void filtraPorIngredienteEPeriodo() {
        long farinha = ingredient("Farinha", Unit.G, false);
        long leite = ingredient("Leite", Unit.ML, false);

        purchaseService.register(new PurchaseRequest(LocalDate.now().minusDays(5), null,
                List.of(line(farinha, "1", Unit.KG, "5"))));
        PurchaseResponse recent = purchaseService.register(new PurchaseRequest(LocalDate.now().minusDays(1), null,
                List.of(line(farinha, "1", Unit.KG, "6"))));

        List<PurchaseResponse> all = purchaseService.list(null, null, farinha);
        assertEquals(2, all.size());
        assertEquals(recent.id(), all.get(0).id());          // mais recente primeiro

        assertEquals(1, purchaseService.list(LocalDate.now().minusDays(3), null, farinha).size());
        assertEquals(0, purchaseService.list(null, null, leite).size());
    }

    @Test
    void compraInexistenteLancaNotFound() {
        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> purchaseService.findById(999999));
        assertEquals("PURCHASE_NOT_FOUND", ex.getCode());
    }
}