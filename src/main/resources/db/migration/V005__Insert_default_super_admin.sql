-- Password: Admin123! (hashed with BCrypt)
INSERT INTO staff (
    id,
    name,
    email,
    password_hash,
    role,
    status,
    two_factor_enabled,
    failed_login_attempts,
    last_password_change,
    created_at,
    updated_at,
    version
) VALUES (
             'super-admin-id',
             'System Administrator',
             'admin@transport.local',
             '$2a$10$N9qo8uLOickgx2ZMRZoMye1VBdGCeGcLYIYUG6c5.UGUfSNjfcB3e',
             'SUPER_ADMIN',
             'ACTIVE',
             false,
             0,
             CURRENT_TIMESTAMP,
             CURRENT_TIMESTAMP,
             CURRENT_TIMESTAMP,
             0
         );
