package br.com.salgados.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.com.salgados.domain.enums.RecipeStatus;
import br.com.salgados.domain.enums.Unit;
import br.com.salgados.dto.request.IngredientRequest;
import br.com.salgados.dto.request.ProductRequest;
import br.com.salgados.dto.request.RecipeItemRequest;
import br.com.salgados.dto.request.RecipeRequest;
import br.com.salgados.dto.response.RecipeResponse;
import br.com.salgados.exception.BusinessException;
import br.com.salgados.exception.NotFoundException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecipeServiceIntegrationTest {

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private IngredientService ingredientService;

    @Autowired
    private ProductService productService;

    @Autowired
    private JdbcTemplate jdbc;

    // ---------- auxiliares ----------

    private long ingredient(String name, Unit base, boolean allowFraction) {
        return ingredientService.create(new IngredientRequest(
                name, "Teste", base, allowFraction, BigDecimal.ZERO, null)).id();
    }

    private long product(String name) {
        return productService.create(new ProductRequest(name, "Fritos", null, null)).id();
    }

    private static RecipeItemRequest item(long ingredientId, String quantity, Unit unit) {
        return new RecipeItemRequest(ingredientId, new BigDecimal(quantity), unit);
    }

    private static RecipeRequest recipe(String yield, RecipeItemRequest... items) {
        return new RecipeRequest(new BigDecimal(yield), List.of(items));
    }

    private static void assertValor(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " mas foi " + real);
    }

    // ---------- testes ----------

    @Test
    void criaPrimeiraVersaoAtivaComItensConvertidosParaUnidadeBase() {
        long farinha = ingredient("Farinha", Unit.G, false);
        long leite = ingredient("Leite", Unit.ML, false);
        long coxinha = product("Coxinha");

        RecipeResponse r = recipeService.create(coxinha, recipe("100",
                item(farinha, "2", Unit.KG), item(leite, "500", Unit.ML)));

        assertEquals(1, r.version());
        assertEquals(RecipeStatus.ACTIVE, r.status());
        assertEquals("Coxinha", r.productName());
        assertValor("100", r.yieldQuantity());
        assertEquals(2, r.items().size());
        assertValor("2000", r.items().get(0).quantityBase());   // 2 kg -> 2000 g
        assertValor("500", r.items().get(1).quantityBase());
    }

    @Test
    void preservaAUnidadeEmQueOUsuarioDigitou() {
        long farinha = ingredient("Farinha", Unit.G, false);
        long coxinha = product("Coxinha");

        RecipeResponse r = recipeService.create(coxinha, recipe("100", item(farinha, "2.5", Unit.KG)));

        assertEquals(Unit.KG, r.items().get(0).displayUnit());
        assertValor("2.5", r.items().get(0).displayQuantity());
        assertValor("2500", r.items().get(0).quantityBase());
    }

    @Test
    void novaVersaoArquivaAAnteriorEPreservaSeusItens() {
        long farinha = ingredient("Farinha", Unit.G, false);
        long coxinha = product("Coxinha");

        RecipeResponse v1 = recipeService.create(coxinha, recipe("100", item(farinha, "2", Unit.KG)));
        RecipeResponse v2 = recipeService.create(coxinha, recipe("120", item(farinha, "3", Unit.KG)));

        assertEquals(2, v2.version());
        assertEquals(RecipeStatus.ACTIVE, v2.status());

        List<RecipeResponse> all = recipeService.listByProduct(coxinha);
        assertEquals(2, all.size());
        assertEquals(2, all.get(0).version());                    // mais nova primeiro
        assertEquals(RecipeStatus.ACTIVE, all.get(0).status());
        assertEquals(RecipeStatus.ARCHIVED, all.get(1).status());

        // a versão 1 continua exatamente como foi gravada
        RecipeResponse old = recipeService.findById(v1.id());
        assertValor("100", old.yieldQuantity());
        assertValor("2000", old.items().get(0).quantityBase());
    }

    @Test
    void numeracaoDeVersaoEIndependentePorProduto() {
        long farinha = ingredient("Farinha", Unit.G, false);
        long coxinha = product("Coxinha");
        long empada = product("Empada");

        recipeService.create(coxinha, recipe("100", item(farinha, "2", Unit.KG)));
        recipeService.create(coxinha, recipe("100", item(farinha, "2", Unit.KG)));
        RecipeResponse primeiraDaEmpada = recipeService.create(empada, recipe("50", item(farinha, "1", Unit.KG)));

        assertEquals(1, primeiraDaEmpada.version());
        // a receita de um produto não arquiva a do outro
        assertEquals(RecipeStatus.ACTIVE, primeiraDaEmpada.status());
        assertEquals(RecipeStatus.ACTIVE, recipeService.listByProduct(coxinha).get(0).status());
    }

    @Test
    void versaoInvalidaNaoMexeNaVersaoAtiva() {
        long farinha = ingredient("Farinha", Unit.G, false);
        long coxinha = product("Coxinha");
        recipeService.create(coxinha, recipe("100", item(farinha, "2", Unit.KG)));

        assertThrows(BusinessException.class,
                () -> recipeService.create(coxinha, recipe("100", item(999999, "1", Unit.KG))));

        List<RecipeResponse> all = recipeService.listByProduct(coxinha);
        assertEquals(1, all.size());
        assertEquals(RecipeStatus.ACTIVE, all.get(0).status());
    }

    @Test
    void rejeitaReceitaSemItens() {
        long coxinha = product("Coxinha");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recipeService.create(coxinha, new RecipeRequest(new BigDecimal("100"), List.of())));
        assertEquals("RECIPE_WITHOUT_ITEMS", ex.getCode());
    }

    @Test
    void rejeitaIngredienteRepetido() {
        long farinha = ingredient("Farinha", Unit.G, false);
        long coxinha = product("Coxinha");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recipeService.create(coxinha, recipe("100",
                        item(farinha, "1", Unit.KG), item(farinha, "500", Unit.G))));
        assertEquals("DUPLICATE_RECIPE_ITEM", ex.getCode());
    }

    @Test
    void rejeitaIngredienteInativo() {
        long farinha = ingredient("Farinha", Unit.G, false);
        ingredientService.setActive(farinha, false);
        long coxinha = product("Coxinha");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recipeService.create(coxinha, recipe("100", item(farinha, "1", Unit.KG))));
        assertEquals("INGREDIENT_INACTIVE", ex.getCode());
    }

    @Test
    void rejeitaIngredienteInexistente() {
        long coxinha = product("Coxinha");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recipeService.create(coxinha, recipe("100", item(999999, "1", Unit.KG))));
        assertEquals("INGREDIENT_NOT_FOUND", ex.getCode());
    }

    @Test
    void bloqueiaConversaoDeGramaParaMililitro() {
        long leite = ingredient("Leite", Unit.ML, false);
        long coxinha = product("Coxinha");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recipeService.create(coxinha, recipe("100", item(leite, "500", Unit.G))));
        assertEquals("INVALID_UNIT_CONVERSION", ex.getCode());
    }

    @Test
    void rejeitaFracaoEmIngredienteContadoEmUnidadesInteiras() {
        long ovo = ingredient("Ovo", Unit.UN, false);
        long coxinha = product("Coxinha");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> recipeService.create(coxinha, recipe("100", item(ovo, "1.5", Unit.UN))));
        assertEquals("INVALID_QUANTITY", ex.getCode());
    }

    @Test
    void produtoInexistenteLancaNotFound() {
        long farinha = ingredient("Farinha", Unit.G, false);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> recipeService.create(999999, recipe("100", item(farinha, "1", Unit.KG))));
        assertEquals("PRODUCT_NOT_FOUND", ex.getCode());
    }

    @Test
    void bancoImpedeDuasVersoesAtivasDoMesmoProduto() {
        long coxinha = product("Coxinha");
        String insert = "INSERT INTO recipe (product_id, version, yield_quantity, status, created_at) "
                + "VALUES (?, ?, '10', 'ACTIVE', '2026-11-04T10:00:00')";

        jdbc.update(insert, coxinha, 1);
        assertThrows(DataAccessException.class, () -> jdbc.update(insert, coxinha, 2));
    }
}