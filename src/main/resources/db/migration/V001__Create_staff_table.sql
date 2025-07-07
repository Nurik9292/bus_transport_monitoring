CREATE TABLE staff (
                       id VARCHAR(36) PRIMARY KEY,

                       name VARCHAR(100) NOT NULL,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL,
                       status VARCHAR(30) NOT NULL,

                       avatar_filename VARCHAR(255),
                       avatar_content_type VARCHAR(50),
                       avatar_size_bytes BIGINT,

                       two_factor_enabled BOOLEAN NOT NULL DEFAULT false,
                       failed_login_attempts INTEGER NOT NULL DEFAULT 0,
                       last_password_change TIMESTAMP NOT NULL,
                       last_login_at TIMESTAMP,

                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       created_by VARCHAR(36),
                       updated_by VARCHAR(36),

                       version BIGINT NOT NULL DEFAULT 0,

                       CONSTRAINT chk_staff_role CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'OPERATOR', 'VIEWER')),
                       CONSTRAINT chk_staff_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION')),
                       CONSTRAINT chk_staff_name_length CHECK (LENGTH(name) >= 2 AND LENGTH(name) <= 100),
                       CONSTRAINT chk_staff_failed_attempts CHECK (failed_login_attempts >= 0 AND failed_login_attempts <= 10)
);

CREATE INDEX idx_staff_email ON staff (email);
CREATE INDEX idx_staff_role ON staff (role);
CREATE INDEX idx_staff_status ON staff (status);
CREATE INDEX idx_staff_created_at ON staff (created_at);

CREATE INDEX idx_staff_status_role ON staff (status, role);

COMMENT ON TABLE staff IS 'Staff management table for transport monitoring system';
COMMENT ON COLUMN staff.id IS 'Unique staff identifier (UUID)';
COMMENT ON COLUMN staff.role IS 'Staff role: SUPER_ADMIN, ADMIN, OPERATOR, VIEWER';
COMMENT ON COLUMN staff.status IS 'Account status: ACTIVE, INACTIVE, SUSPENDED, PENDING_VERIFICATION';
COMMENT ON COLUMN staff.failed_login_attempts IS 'Number of consecutive failed login attempts';
COMMENT ON COLUMN staff.version IS 'Optimistic locking version number';
