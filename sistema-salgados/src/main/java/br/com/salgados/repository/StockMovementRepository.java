package br.com.salgados.repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import br.com.salgados.domain.enums.MovementType;
import br.com.salgados.domain.enums.SourceType;
import br.com.salgados.domain.model.StockMovement;
import br.com.salgados.util.Decimals;

@Repository
public class StockMovementRepository {

    private static final String COLUMNS =
            "id, ingredient_id, movement_type, quantity, source_type, source_id, occurred_at, reason, created_at";

    private static final RowMapper<StockMovement> MAPPER = (rs, rowNum) -> {
        long sourceIdValue = rs.getLong("source_id");
        Long sourceId = rs.wasNull() ? null : sourceIdValue;
        return new StockMovement(
                rs.getLong("id"),
                rs.getLong("ingredient_id"),
                MovementType.valueOf(rs.getString("movement_type")),
                Decimals.fromDb(rs.getString("quantity")),
                SourceType.valueOf(rs.getString("source_type")),
                sourceId,
                LocalDateTime.parse(rs.getString("occurred_at")),
                rs.getString("reason"),
                LocalDateTime.parse(rs.getString("created_at")));
    };

    private final JdbcTemplate jdbc;

    public StockMovementRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(StockMovement m) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO stock_movement
                        (ingredient_id, movement_type, quantity, source_type, source_id,
                         occurred_at, reason, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, m.ingredientId());
            ps.setString(2, m.movementType().name());
            ps.setString(3, Decimals.toDb(m.quantity()));
            ps.setString(4, m.sourceType().name());
            if (m.sourceId() == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setLong(5, m.sourceId());
            }
            ps.setString(6, m.occurredAt().toString());
            ps.setString(7, m.reason());
            ps.setString(8, m.createdAt().toString());
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public List<StockMovement> findBySource(SourceType sourceType, long sourceId) {
        return jdbc.query("SELECT " + COLUMNS
                + " FROM stock_movement WHERE source_type = ? AND source_id = ? ORDER BY id",
                MAPPER, sourceType.name(), sourceId);
    }

    /** Soma dos movimentos de um ingrediente: deve sempre coincidir com ingredient.current_stock. */
    public BigDecimal sumQuantity(long ingredientId) {
        return jdbc.queryForList(
                        "SELECT quantity FROM stock_movement WHERE ingredient_id = ?", String.class, ingredientId)
                .stream()
                .map(Decimals::fromDb)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}