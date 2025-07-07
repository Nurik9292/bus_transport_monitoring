CREATE INDEX CONCURRENTLY idx_staff_active_email
    ON staff (email)
    WHERE status = 'ACTIVE';

CREATE INDEX CONCURRENTLY idx_staff_active_role
    ON staff (role)
    WHERE status = 'ACTIVE';

CREATE INDEX CONCURRENTLY idx_staff_auth_lookup
    ON staff (email, password_hash, status)
    WHERE status IN ('ACTIVE', 'PENDING_VERIFICATION');

CREATE INDEX CONCURRENTLY idx_staff_sessions_cleanup_active
    ON staff_sessions (expires_at)
    WHERE is_active = true;

CREATE INDEX CONCURRENTLY idx_staff_activity_log_cleanup
    ON staff_activity_log (created_at)
    WHERE created_at > (CURRENT_TIMESTAMP - INTERVAL '1 year');

CREATE INDEX CONCURRENTLY idx_staff_list_covering
    ON staff (status, role, created_at)
    INCLUDE (id, name, email, last_login_at);
