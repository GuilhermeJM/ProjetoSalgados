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

import br.com.salgados.domain.model.Product;
import br.com.salgados.util.Decimals;

@Repository
public class ProductRepository {

    private static final String COLUMNS =
            "id, name, category, description, unit_weight, active, created_at";

    private static final RowMapper<Product> MAPPER = (rs, rowNum) -> {
        String weight = rs.getString("unit_weight");
        return new Product(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getString("description"),
                weight == null ? null : Decimals.fromDb(weight),
                rs.getInt("active") == 1,
                LocalDateTime.parse(rs.getString("created_at")));
    };

    private final JdbcTemplate jdbc;

    public ProductRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Product> findById(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM product WHERE id = ?", MAPPER, id)
                .stream().findFirst();
    }

    public List<Product> search(String search, String category, Boolean active) {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM product WHERE 1 = 1");
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

    public boolean existsByName(String name, Long excludeId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM product WHERE name = ? AND id <> ?",
                Integer.class, name, excludeId == null ? -1L : excludeId);
        return count != null && count > 0;
    }

    public long insert(Product p) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO product (name, category, description, unit_weight, active, created_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, p.name());
            ps.setString(2, p.category());
            ps.setString(3, p.description());
            ps.setString(4, p.unitWeight() == null ? null : Decimals.toDb(p.unitWeight()));
            ps.setInt(5, p.active() ? 1 : 0);
            ps.setString(6, p.createdAt().toString());
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void update(Product p) {
        jdbc.update("""
                UPDATE product
                   SET name = ?, category = ?, description = ?, unit_weight = ?
                 WHERE id = ?
                """,
                p.name(), p.category(), p.description(),
                p.unitWeight() == null ? null : Decimals.toDb(p.unitWeight()), p.id());
    }

    public void setActive(long id, boolean active) {
        jdbc.update("UPDATE product SET active = ? WHERE id = ?", active ? 1 : 0, id);
    }
}