package br.com.salgados.repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
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

import br.com.salgados.domain.enums.MovementType;
import br.com.salgados.domain.enums.SourceType;
import br.com.salgados.domain.enums.Unit;
import br.com.salgados.domain.model.StockMovement;
import br.com.salgados.domain.model.StockMovementDetail;
import br.com.salgados.util.Decimals;

@Repository
public class StockMovementRepository {

    private static final String MOVEMENT_COLUMNS =
            "m.id, m.ingredient_id, m.movement_type, m.quantity, m.source_type, m.source_id, "
            + "m.occurred_at, m.reason, m.created_at";

    private static final String DETAIL_SELECT = "SELECT " + MOVEMENT_COLUMNS
            + ", i.name AS ingredient_name, i.base_unit"
            + " FROM stock_movement m JOIN ingredient i ON i.id = m.ingredient_id";

    private static StockMovement mapMovement(ResultSet rs) throws SQLException {
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
    }

    private static final RowMapper<StockMovement> MAPPER = (rs, rowNum) -> mapMovement(rs);

    private static final RowMapper<StockMovementDetail> DETAIL_MAPPER = (rs, rowNum) ->
            new StockMovementDetail(mapMovement(rs), rs.getString("ingredient_name"),
                    Unit.valueOf(rs.getString("base_unit")));

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
        return jdbc.query("SELECT " + MOVEMENT_COLUMNS
                + " FROM stock_movement m WHERE m.source_type = ? AND m.source_id = ? ORDER BY m.id",
                MAPPER, sourceType.name(), sourceId);
    }

    /** Todos os movimentos de um ingrediente, do mais antigo para o mais novo. */
    public List<StockMovement> findAllByIngredient(long ingredientId) {
        return jdbc.query("SELECT " + MOVEMENT_COLUMNS
                + " FROM stock_movement m WHERE m.ingredient_id = ? ORDER BY m.occurred_at, m.id",
                MAPPER, ingredientId);
    }

    public Optional<StockMovementDetail> findDetailById(long id) {
        return jdbc.query(DETAIL_SELECT + " WHERE m.id = ?", DETAIL_MAPPER, id).stream().findFirst();
    }

    /** Extrato: mais recentes primeiro. O filtro de datas compara só o dia (aaaa-mm-dd). */
    public List<StockMovementDetail> search(Long ingredientId, MovementType type,
                                            LocalDate from, LocalDate to, int limit) {
        StringBuilder sql = new StringBuilder(DETAIL_SELECT + " WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (ingredientId != null) {
            sql.append(" AND m.ingredient_id = ?");
            params.add(ingredientId);
        }
        if (type != null) {
            sql.append(" AND m.movement_type = ?");
            params.add(type.name());
        }
        if (from != null) {
            sql.append(" AND substr(m.occurred_at, 1, 10) >= ?");
            params.add(from.toString());
        }
        if (to != null) {
            sql.append(" AND substr(m.occurred_at, 1, 10) <= ?");
            params.add(to.toString());
        }
        sql.append(" ORDER BY m.occurred_at DESC, m.id DESC LIMIT ?");
        params.add(limit);

        return jdbc.query(sql.toString(), DETAIL_MAPPER, params.toArray());
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