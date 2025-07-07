package tm.ugur.ugur_v3.domain.staffManagement.valueobjects;

import java.util.Objects;

public class SecuritySettings {

    private final boolean twoFactorEnabled;
    private final int failedLoginAttempts;
    private final java.time.LocalDateTime lastPasswordChange;
    private final java.time.LocalDateTime lastLogin;

    private SecuritySettings(boolean twoFactorEnabled, int failedLoginAttempts,
                             java.time.LocalDateTime lastPasswordChange,
                             java.time.LocalDateTime lastLogin) {
        this.twoFactorEnabled = twoFactorEnabled;
        this.failedLoginAttempts = Math.max(0, failedLoginAttempts);
        this.lastPasswordChange = lastPasswordChange;
        this.lastLogin = lastLogin;
    }

    public static SecuritySettings defaultSettings() {
        return new SecuritySettings(false, 0, java.time.LocalDateTime.now(), null);
    }

    public static SecuritySettings create(boolean twoFactorEnabled, int failedLoginAttempts,
                                          java.time.LocalDateTime lastPasswordChange,
                                          java.time.LocalDateTime lastLogin) {
        return new SecuritySettings(twoFactorEnabled, failedLoginAttempts,
                lastPasswordChange, lastLogin);
    }

    public SecuritySettings passwordChanged() {
        return new SecuritySettings(this.twoFactorEnabled, 0,
                java.time.LocalDateTime.now(), this.lastLogin);
    }

    public SecuritySettings loginRecorded() {
        return new SecuritySettings(this.twoFactorEnabled, 0,
                this.lastPasswordChange, java.time.LocalDateTime.now());
    }

    public SecuritySettings failedLoginAttempt() {
        return new SecuritySettings(this.twoFactorEnabled, this.failedLoginAttempts + 1,
                this.lastPasswordChange, this.lastLogin);
    }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public java.time.LocalDateTime getLastPasswordChange() {
        return lastPasswordChange;
    }

    public java.time.LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public boolean isAccountLocked() {
        return failedLoginAttempts >= 5;
    }

    public boolean needsPasswordChange() {
        if (lastPasswordChange == null) return true;
        return lastPasswordChange.isBefore(java.time.LocalDateTime.now().minusDays(90));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SecuritySettings other)) return false;
        return this.twoFactorEnabled == other.twoFactorEnabled &&
                this.failedLoginAttempts == other.failedLoginAttempts &&
                Objects.equals(this.lastPasswordChange, other.lastPasswordChange) &&
                Objects.equals(this.lastLogin, other.lastLogin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(twoFactorEnabled, failedLoginAttempts, lastPasswordChange, lastLogin);
    }

    @Override
    public String toString() {
        return "SecuritySettings(2FA:" + twoFactorEnabled +
                ", FailedAttempts:" + failedLoginAttempts + ")";
    }
}
