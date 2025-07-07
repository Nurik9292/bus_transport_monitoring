CREATE TABLE staff_sessions (
                                id VARCHAR(36) PRIMARY KEY,
                                staff_id VARCHAR(36) NOT NULL,
                                token_hash VARCHAR(255) NOT NULL UNIQUE,
                                expires_at TIMESTAMP NOT NULL,
                                ip_address VARCHAR(45),
                                user_agent TEXT,
                                is_active BOOLEAN NOT NULL DEFAULT true,
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                last_used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_staff_sessions_staff_id
                                    FOREIGN KEY (staff_id) REFERENCES staff(id) ON DELETE CASCADE
);

CREATE INDEX idx_staff_sessions_staff_id ON staff_sessions (staff_id);
CREATE INDEX idx_staff_sessions_token_hash ON staff_sessions (token_hash);
CREATE INDEX idx_staff_sessions_expires_at ON staff_sessions (expires_at);
CREATE INDEX idx_staff_sessions_active ON staff_sessions (is_active);

CREATE INDEX idx_staff_sessions_cleanup ON staff_sessions (expires_at, is_active);

COMMENT ON TABLE staff_sessions IS 'Active JWT sessions for staff members';
COMMENT ON COLUMN staff_sessions.token_hash IS 'Hashed JWT token for security';
COMMENT ON COLUMN staff_sessions.expires_at IS 'Session expiration timestamp';
COMMENT ON COLUMN staff_sessions.is_active IS 'Whether session is currently active';