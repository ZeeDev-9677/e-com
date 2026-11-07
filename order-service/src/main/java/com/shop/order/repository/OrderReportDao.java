package com.shop.order.repository;

import com.shop.order.dto.OrderSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Thin wrapper over PostgreSQL function / procedure (see V2 migration). */
@Repository
@RequiredArgsConstructor
public class OrderReportDao {
    private final JdbcTemplate jdbc;

    public List<OrderSummary> summaryFor(UUID userId) {
        return jdbc.query("select status, order_count, total_amount from fn_user_order_summary(?)",
                (rs, i) -> new OrderSummary(rs.getString("status"), rs.getLong("order_count"),
                        rs.getBigDecimal("total_amount")), userId);
    }

    public void cancelStale(int minutes) {
        jdbc.update("CALL sp_cancel_stale_orders(?)", minutes);
    }
}
