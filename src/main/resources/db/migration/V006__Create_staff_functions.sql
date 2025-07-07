CREATE OR REPLACE FUNCTION update_staff_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    NEW.version = OLD.version + 1;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_staff_updated_at
    BEFORE UPDATE ON staff
    FOR EACH ROW
    EXECUTE FUNCTION update_staff_updated_at();

CREATE OR REPLACE FUNCTION cleanup_expired_sessions()
RETURNS INTEGER AS $$
DECLARE
deleted_count INTEGER;
BEGIN
DELETE FROM staff_sessions
WHERE expires_at < CURRENT_TIMESTAMP;

GET DIAGNOSTICS deleted_count = ROW_COUNT;
RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION cleanup_old_activity_logs()
RETURNS INTEGER AS $$
DECLARE
deleted_count INTEGER;
BEGIN
DELETE FROM staff_activity_log
WHERE created_at < (CURRENT_TIMESTAMP - INTERVAL '1 year');

GET DIAGNOSTICS deleted_count = ROW_COUNT;
RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_staff_statistics()
RETURNS TABLE (
    total_staff INTEGER,
    active_staff INTEGER,
    inactive_staff INTEGER,
    locked_accounts INTEGER,
    recent_logins INTEGER
) AS $$
BEGIN
RETURN QUERY
SELECT
    COUNT(*)::INTEGER as total_staff,
        COUNT(*) FILTER (WHERE status = 'ACTIVE')::INTEGER as active_staff,
        COUNT(*) FILTER (WHERE status = 'INACTIVE')::INTEGER as inactive_staff,
        COUNT(*) FILTER (WHERE failed_login_attempts >= 5)::INTEGER as locked_accounts,
        COUNT(*) FILTER (WHERE last_login_at > CURRENT_TIMESTAMP - INTERVAL '7 days')::INTEGER as recent_logins
FROM staff;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION update_staff_updated_at() IS 'Automatically updates updated_at and version on staff table changes';
COMMENT ON FUNCTION cleanup_expired_sessions() IS 'Removes expired staff sessions, returns count of deleted records';
COMMENT ON FUNCTION cleanup_old_activity_logs() IS 'Removes activity logs older than 1 year, returns count of deleted records';
COMMENT ON FUNCTION get_staff_statistics() IS 'Returns comprehensive staff statistics for dashboard';
