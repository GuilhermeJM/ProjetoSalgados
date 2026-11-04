package br.com.salgados.domain.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.salgados.domain.enums.RecipeStatus;
import br.com.salgados.domain.model.Ingredient;
import br.com.salgados.domain.model.Product;
import br.com.salgados.domain.model.Recipe;
import br.com.salgados.domain.model.RecipeItem;
import br.com.salgados.dto.request.RecipeItemRequest;
import br.com.salgados.dto.request.RecipeRequest;
import br.com.salgados.dto.response.RecipeItemResponse;
import br.com.salgados.dto.response.RecipeResponse;
import br.com.salgados.exception.BusinessException;
import br.com.salgados.exception.NotFoundException;
import br.com.salgados.repository.IngredientRepository;
import br.com.salgados.repository.ProductRepository;
import br.com.salgados.repository.RecipeRepository;

@Service
public class RecipeService {

    private static final String YIELD_UNIT = "UN";

    private final RecipeRepository recipes;
    private final ProductRepository products;
    private final IngredientRepository ingredients;
    private final UnitConversionService conversion;

    public RecipeService(RecipeRepository recipes, ProductRepository products,
                         IngredientRepository ingredients, UnitConversionService conversion) {
        this.recipes = recipes;
        this.products = products;
        this.ingredients = ingredients;
        this.conversion = conversion;
    }

    public List<RecipeResponse> listByProduct(long productId) {
        Product product = getProduct(productId);
        return recipes.findByProduct(productId).stream()
                .map(r -> toResponse(r, product.name()))
                .toList();
    }

    public RecipeResponse findById(long id) {
        Recipe recipe = recipes.findById(id).orElseThrow(() ->
                new NotFoundException("RECIPE_NOT_FOUND", "Receita não encontrada."));
        return toResponse(recipe, getProduct(recipe.productId()).name());
    }

    /** Cria uma nova versão ativa para o produto e arquiva a anterior. */
    @Transactional
    public RecipeResponse create(long productId, RecipeRequest request) {
        getProduct(productId);

        if (request.items().isEmpty()) {
            throw new BusinessException("RECIPE_WITHOUT_ITEMS",
                    "A receita precisa ter pelo menos um ingrediente.");
        }

        // 1) validar tudo, sem gravar nada
        Set<Long> seen = new HashSet<>();
        List<RecipeItem> items = new ArrayList<>();
        for (RecipeItemRequest itemRequest : request.items()) {
            Ingredient ingredient = ingredients.findById(itemRequest.ingredientId()).orElseThrow(() ->
                    new BusinessException("INGREDIENT_NOT_FOUND",
                            "Ingrediente não encontrado (id " + itemRequest.ingredientId() + ")."));

            if (!ingredient.active()) {
                throw new BusinessException("INGREDIENT_INACTIVE",
                        "O ingrediente \"" + ingredient.name() + "\" está inativo. "
                        + "Reative-o ou retire-o da receita.");
            }
            if (!seen.add(ingredient.id())) {
                throw new BusinessException("DUPLICATE_RECIPE_ITEM",
                        "O ingrediente \"" + ingredient.name() + "\" aparece mais de uma vez. "
                        + "Some as quantidades em uma única linha.");
            }

            BigDecimal quantityBase = conversion.toBase(
                    itemRequest.quantity(), itemRequest.unit(), ingredient.baseUnit());
            conversion.validateQuantity(quantityBase, ingredient.baseUnit(), ingredient.allowFraction());

            items.add(new RecipeItem(null, null, ingredient.id(), ingredient.name(),
                    ingredient.baseUnit(), quantityBase, itemRequest.unit()));
        }

        // 2) gravar: arquivar a ativa ANTES de inserir a nova (índice único parcial)
        recipes.archiveActive(productId);
        int version = recipes.nextVersion(productId);
        long recipeId = recipes.insert(new Recipe(null, productId, version,
                request.yieldQuantity(), YIELD_UNIT, RecipeStatus.ACTIVE, LocalDateTime.now()));
        items.forEach(item -> recipes.insertItem(recipeId, item));

        return findById(recipeId);
    }

    // ---------- auxiliares ----------

    private Product getProduct(long productId) {
        return products.findById(productId).orElseThrow(() ->
                new NotFoundException("PRODUCT_NOT_FOUND", "Produto não encontrado."));
    }

    private RecipeResponse toResponse(Recipe recipe, String productName) {
        List<RecipeItemResponse> items = recipes.findItems(recipe.id()).stream()
                .map(i -> new RecipeItemResponse(
                        i.ingredientId(), i.ingredientName(), i.baseUnit(), i.quantityBase(),
                        conversion.fromBase(i.quantityBase(), i.displayUnit()), i.displayUnit()))
                .toList();
        return new RecipeResponse(recipe.id(), recipe.productId(), productName, recipe.version(),
                recipe.yieldQuantity(), recipe.yieldUnit(), recipe.status(), recipe.createdAt(), items);
    }
}