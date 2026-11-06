package br.com.salgados.repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
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

import br.com.salgados.domain.enums.Unit;
import br.com.salgados.domain.model.PurchaseLineCost;
import br.com.salgados.domain.model.Purchase;
import br.com.salgados.domain.model.PurchaseItem;
import br.com.salgados.util.Decimals;

@Repository
public class PurchaseRepository {

    private static final String COLUMNS = "id, purchase_date, notes, total_amount, created_at";

    private static final RowMapper<Purchase> PURCHASE_MAPPER = (rs, rowNum) -> new Purchase(
            rs.getLong("id"),
            LocalDate.parse(rs.getString("purchase_date")),
            rs.getString("notes"),
            Decimals.fromDb(rs.getString("total_amount")),
            LocalDateTime.parse(rs.getString("created_at")));

    private static final RowMapper<PurchaseItem> ITEM_MAPPER = (rs, rowNum) -> new PurchaseItem(
            rs.getLong("id"),
            rs.getLong("purchase_id"),
            rs.getLong("ingredient_id"),
            rs.getString("ingredient_name"),
            Unit.valueOf(rs.getString("base_unit")),
            Decimals.fromDb(rs.getString("quantity_base")),
            Decimals.fromDb(rs.getString("total_cost")),
            Decimals.fromDb(rs.getString("unit_cost_base")));

    private final JdbcTemplate jdbc;

    public PurchaseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Purchase> findById(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM purchase WHERE id = ?", PURCHASE_MAPPER, id)
                .stream().findFirst();
    }

    /** Mais recentes primeiro. Datas ISO (aaaa-mm-dd) comparam corretamente como texto. */
    public List<Purchase> search(LocalDate from, LocalDate to, Long ingredientId) {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM purchase WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (from != null) {
            sql.append(" AND purchase_date >= ?");
            params.add(from.toString());
        }
        if (to != null) {
            sql.append(" AND purchase_date <= ?");
            params.add(to.toString());
        }
        if (ingredientId != null) {
            sql.append(" AND id IN (SELECT purchase_id FROM purchase_item WHERE ingredient_id = ?)");
            params.add(ingredientId);
        }
        sql.append(" ORDER BY purchase_date DESC, id DESC");

        return jdbc.query(sql.toString(), PURCHASE_MAPPER, params.toArray());
    }

    public List<PurchaseItem> findItems(long purchaseId) {
        return jdbc.query("""
                SELECT pi.id, pi.purchase_id, pi.ingredient_id, i.name AS ingredient_name,
                       i.base_unit, pi.quantity_base, pi.total_cost, pi.unit_cost_base
                  FROM purchase_item pi
                  JOIN ingredient i ON i.id = pi.ingredient_id
                 WHERE pi.purchase_id = ?
                 ORDER BY pi.id
                """, ITEM_MAPPER, purchaseId);
    }

    public long insert(Purchase p) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO purchase (purchase_date, notes, total_amount, created_at)
                    VALUES (?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, p.purchaseDate().toString());
            ps.setString(2, p.notes());
            ps.setString(3, Decimals.toDb(p.totalAmount()));
            ps.setString(4, p.createdAt().toString());
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void insertItem(long purchaseId, long ingredientId, BigDecimal quantityBase,
                           BigDecimal totalCost, BigDecimal unitCostBase) {
        jdbc.update("""
                INSERT INTO purchase_item (purchase_id, ingredient_id, quantity_base, total_cost, unit_cost_base)
                VALUES (?, ?, ?, ?, ?)
                """,
                purchaseId, ingredientId, Decimals.toDb(quantityBase),
                Decimals.toDb(totalCost), Decimals.toDb(unitCostBase));
    }
    
        /** Custo de cada linha de compra de um ingrediente, na ordem de gravação. Usado no recálculo. */
    public List<PurchaseLineCost> findLineCosts(long ingredientId) {
        return jdbc.query("""
                SELECT purchase_id, quantity_base, total_cost
                  FROM purchase_item
                 WHERE ingredient_id = ?
                 ORDER BY id
                """,
                (rs, rowNum) -> new PurchaseLineCost(
                        rs.getLong("purchase_id"),
                        Decimals.fromDb(rs.getString("quantity_base")),
                        Decimals.fromDb(rs.getString("total_cost"))),
                ingredientId);
    }
}