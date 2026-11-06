package br.com.salgados.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import br.com.salgados.domain.enums.Unit;
import br.com.salgados.domain.model.Ingredient;
import br.com.salgados.util.Decimals;

@Repository
public class IngredientRepository {

    private static final String COLUMNS = "id, name, category, base_unit, allow_fraction, "
            + "minimum_stock, current_stock, current_average_cost, active, created_at";

    private static final RowMapper<Ingredient> MAPPER = (rs, rowNum) -> new Ingredient(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("category"),
            Unit.valueOf(rs.getString("base_unit")),
            rs.getInt("allow_fraction") == 1,
            Decimals.fromDb(rs.getString("minimum_stock")),
            Decimals.fromDb(rs.getString("current_stock")),
            Decimals.fromDb(rs.getString("current_average_cost")),
            rs.getInt("active") == 1,
            LocalDateTime.parse(rs.getString("created_at")));

    private final JdbcTemplate jdbc;

    public IngredientRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Ingredient> findById(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM ingredient WHERE id = ?", MAPPER, id)
                .stream().findFirst();
    }

    public List<Ingredient> search(String search, String category, Boolean active) {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM ingredient WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND name LIKE ?");
            params.add("%" + search.trim() + "%");
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND category = ? COLLATE NOCASE");
            params.add(category.trim());
        }
        if (active != null) {
            sql.append(" AND active = ?");
            params.add(active ? 1 : 0);
        }
        sql.append(" ORDER BY name COLLATE NOCASE");

        return jdbc.query(sql.toString(), MAPPER, params.toArray());
    }

    /** A coluna name é NOCASE, então a comparação ignora maiúsculas/minúsculas. */
    public boolean existsByName(String name, Long excludeId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ingredient WHERE name = ? AND id <> ?",
                Integer.class, name, excludeId == null ? -1L : excludeId);
        return count != null && count > 0;
    }

    /** Verdadeiro se o ingrediente já aparece em movimentos, compras, receitas ou produções. */
    public boolean hasHistory(long id) {
        Integer count = jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM stock_movement WHERE ingredient_id = ?)
                     + (SELECT COUNT(*) FROM purchase_item  WHERE ingredient_id = ?)
                     + (SELECT COUNT(*) FROM recipe_item    WHERE ingredient_id = ?)
                     + (SELECT COUNT(*) FROM production_item WHERE ingredient_id = ?)
                """, Integer.class, id, id, id, id);
        return count != null && count > 0;
    }

    public long insert(Ingredient i) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO ingredient
                        (name, category, base_unit, allow_fraction, minimum_stock, active, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, i.name());
            ps.setString(2, i.category());
            ps.setString(3, i.baseUnit().name());
            ps.setInt(4, i.allowFraction() ? 1 : 0);
            ps.setString(5, Decimals.toDb(i.minimumStock()));
            ps.setInt(6, i.active() ? 1 : 0);
            ps.setString(7, i.createdAt().toString());
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void update(Ingredient i) {
        jdbc.update("""
                UPDATE ingredient
                   SET name = ?, category = ?, base_unit = ?, allow_fraction = ?, minimum_stock = ?
                 WHERE id = ?
                """,
                i.name(), i.category(), i.baseUnit().name(), i.allowFraction() ? 1 : 0,
                Decimals.toDb(i.minimumStock()), i.id());
    }

    public void setActive(long id, boolean active) {
        jdbc.update("UPDATE ingredient SET active = ? WHERE id = ?", active ? 1 : 0, id);
    }

        /** Atualiza o saldo e o custo médio. Chamado só pelos serviços de compra, produção e ajuste. */
    public void updateStockAndCost(long id, java.math.BigDecimal stock, java.math.BigDecimal averageCost) {
        jdbc.update("UPDATE ingredient SET current_stock = ?, current_average_cost = ? WHERE id = ?",
                Decimals.toDb(stock), Decimals.toDb(averageCost), id);
    }

}