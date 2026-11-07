-- Stored procedure (PostgreSQL 11+ CALL). Set-based: one statement instead of loading N entities in Java.
CREATE OR REPLACE PROCEDURE sp_deactivate_dormant_users(p_days INT)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE users
       SET active = FALSE
     WHERE active
       AND COALESCE(last_login_at, created_at) < now() - make_interval(days => p_days);
END;
$$;
