-- Reporting function: aggregation stays next to the data (1 round trip, uses ix_orders_user_created)
CREATE OR REPLACE FUNCTION fn_user_order_summary(p_user_id UUID)
RETURNS TABLE (status VARCHAR, order_count BIGINT, total_amount NUMERIC)
LANGUAGE sql STABLE
AS $$
    SELECT o.status, COUNT(*), COALESCE(SUM(o.total_amount), 0)
      FROM orders o
     WHERE o.user_id = p_user_id
     GROUP BY o.status
     ORDER BY o.status;
$$;

-- Maintenance procedure
CREATE OR REPLACE PROCEDURE sp_cancel_stale_orders(p_minutes INT)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE orders
       SET status = 'CANCELLED', updated_at = now(), version = version + 1
     WHERE status = 'PENDING'
       AND created_at < now() - make_interval(mins => p_minutes);
END;
$$;
