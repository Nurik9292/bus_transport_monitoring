CREATE VIEW staff_read_model AS
SELECT
    s.id,
    s.name,
    s.email,
    s.role,
    s.status,
    s.avatar_filename,
    s.avatar_content_type,
    s.two_factor_enabled,
    s.failed_login_attempts,
    s.last_login_at,
    s.created_at,
    s.updated_at,

    CASE
        WHEN s.avatar_filename IS NOT NULL
            THEN '/api/admin/staff/avatar/' || s.avatar_filename
        ELSE NULL
        END as avatar_url,

    CASE
        WHEN s.failed_login_attempts >= 5 THEN true
        ELSE false
        END as is_locked,

    CASE
        WHEN s.last_password_change < (CURRENT_TIMESTAMP - INTERVAL '90 days')
            THEN true
        ELSE false
        END as password_expired,

    CASE
        WHEN s.last_login_at > (CURRENT_TIMESTAMP - INTERVAL '30 days')
            THEN true
        ELSE false
        END as recently_active,

    COALESCE(activity_stats.login_count, 0) as total_logins,
    activity_stats.last_activity_at,

    COALESCE(session_stats.active_sessions, 0) as active_sessions

FROM staff s

         LEFT JOIN (
    SELECT
        staff_id,
        COUNT(*) FILTER (WHERE action_type = 'LOGIN') as login_count,
            MAX(created_at) as last_activity_at
    FROM staff_activity_log
    GROUP BY staff_id
) activity_stats ON s.id = activity_stats.staff_id

         LEFT JOIN (
    SELECT
        staff_id,
        COUNT(*) as active_sessions
    FROM staff_sessions
    WHERE is_active = true AND expires_at > CURRENT_TIMESTAMP
    GROUP BY staff_id
) session_stats ON s.id = session_stats.staff_id;

COMMENT ON VIEW staff_read_model IS 'Optimized read model for staff list queries with calculated fields';