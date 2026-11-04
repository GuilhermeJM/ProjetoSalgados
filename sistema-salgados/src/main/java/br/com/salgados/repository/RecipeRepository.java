package br.com.salgados.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import br.com.salgados.domain.enums.RecipeStatus;
import br.com.salgados.domain.enums.Unit;
import br.com.salgados.domain.model.Recipe;
import br.com.salgados.domain.model.RecipeItem;
import br.com.salgados.util.Decimals;

@Repository
public class RecipeRepository {

    private static final String COLUMNS =
            "id, product_id, version, yield_quantity, yield_unit, status, created_at";

    private static final RowMapper<Recipe> RECIPE_MAPPER = (rs, rowNum) -> new Recipe(
            rs.getLong("id"),
            rs.getLong("product_id"),
            rs.getInt("version"),
            Decimals.fromDb(rs.getString("yield_quantity")),
            rs.getString("yield_unit"),
            RecipeStatus.valueOf(rs.getString("status")),
            LocalDateTime.parse(rs.getString("created_at")));

    private static final RowMapper<RecipeItem> ITEM_MAPPER = (rs, rowNum) -> new RecipeItem(
            rs.getLong("id"),
            rs.getLong("recipe_id"),
            rs.getLong("ingredient_id"),
            rs.getString("ingredient_name"),
            Unit.valueOf(rs.getString("base_unit")),
            Decimals.fromDb(rs.getString("quantity_base")),
            Unit.valueOf(rs.getString("display_unit")));

    private final JdbcTemplate jdbc;

    public RecipeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Recipe> findById(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM recipe WHERE id = ?", RECIPE_MAPPER, id)
                .stream().findFirst();
    }

    /** Da versão mais nova para a mais antiga. */
    public List<Recipe> findByProduct(long productId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM recipe WHERE product_id = ? ORDER BY version DESC",
                RECIPE_MAPPER, productId);
    }

    /** Itens na ordem em que foram cadastrados. */
    public List<RecipeItem> findItems(long recipeId) {
        return jdbc.query("""
                SELECT ri.id, ri.recipe_id, ri.ingredient_id, i.name AS ingredient_name,
                       i.base_unit, ri.quantity_base, ri.display_unit
                  FROM recipe_item ri
                  JOIN ingredient i ON i.id = ri.ingredient_id
                 WHERE ri.recipe_id = ?
                 ORDER BY ri.id
                """, ITEM_MAPPER, recipeId);
    }

    public int nextVersion(long productId) {
        Integer next = jdbc.queryForObject(
                "SELECT COALESCE(MAX(version), 0) + 1 FROM recipe WHERE product_id = ?",
                Integer.class, productId);
        return Objects.requireNonNull(next);
    }

    /** Deve rodar ANTES de inserir a nova versão ativa (o índice único só admite uma). */
    public void archiveActive(long productId) {
        jdbc.update("UPDATE recipe SET status = 'ARCHIVED' WHERE product_id = ? AND status = 'ACTIVE'",
                productId);
    }

    public long insert(Recipe r) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO recipe (product_id, version, yield_quantity, yield_unit, status, created_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, r.productId());
            ps.setInt(2, r.version());
            ps.setString(3, Decimals.toDb(r.yieldQuantity()));
            ps.setString(4, r.yieldUnit());
            ps.setString(5, r.status().name());
            ps.setString(6, r.createdAt().toString());
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void insertItem(long recipeId, RecipeItem item) {
        jdbc.update("""
                INSERT INTO recipe_item (recipe_id, ingredient_id, quantity_base, display_unit)
                VALUES (?, ?, ?, ?)
                """,
                recipeId, item.ingredientId(), Decimals.toDb(item.quantityBase()), item.displayUnit().name());
    }
}