CREATE VIEW staff_table_stats AS
SELECT
    schemaname,
    tablename,
    n_tup_ins as inserts,
    n_tup_upd as updates,
    n_tup_del as deletes,
    n_live_tup as live_tuples,
    n_dead_tup as dead_tuples,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables
WHERE tablename IN ('staff', 'staff_sessions', 'staff_activity_log');

CREATE VIEW staff_index_stats AS
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan as index_scans,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched,
    ROUND((idx_scan::numeric / NULLIF(idx_scan + seq_scan, 0)) * 100, 2) as index_usage_pct
FROM pg_stat_user_indexes i
         JOIN pg_stat_user_tables t ON i.relid = t.relid
WHERE i.tablename IN ('staff', 'staff_sessions', 'staff_activity_log')
ORDER BY idx_scan DESC;

CREATE VIEW staff_system_health AS
SELECT
    (SELECT COUNT(*) FROM staff WHERE status = 'ACTIVE') as active_staff,
    (SELECT COUNT(*) FROM staff WHERE failed_login_attempts >= 5) as locked_accounts,
    (SELECT COUNT(*) FROM staff_sessions WHERE is_active = true AND expires_at > CURRENT_TIMESTAMP) as active_sessions,
    (SELECT COUNT(*) FROM staff_activity_log WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '24 hours') as actions_last_24h,
        (SELECT AVG(execution_time_ms) FROM staff_activity_log WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '1 hour') as avg_response_time_ms;

COMMENT ON VIEW staff_table_stats IS 'PostgreSQL statistics for staff-related tables';
COMMENT ON VIEW staff_index_stats IS 'Index usage statistics for performance monitoring';
COMMENT ON VIEW staff_system_health IS 'Real-time health metrics for staff management system';
