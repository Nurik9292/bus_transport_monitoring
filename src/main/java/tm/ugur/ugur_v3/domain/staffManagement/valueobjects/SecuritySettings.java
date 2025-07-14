package tm.ugur.ugur_v3.domain.staffManagement.valueobjects;

import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;


public final class SecuritySettings extends ValueObject {

    private final boolean twoFactorEnabled;
    private final int failedLoginAttempts;
    private final int maxFailedAttempts;
    private final Timestamp passwordExpiresAt;
    private final int sessionTimeoutMinutes;
    private final boolean ipWhitelistEnabled;

    private SecuritySettings(boolean twoFactorEnabled, int failedLoginAttempts, int maxFailedAttempts,
                             Timestamp passwordExpiresAt, int sessionTimeoutMinutes, boolean ipWhitelistEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
        this.failedLoginAttempts = Math.max(0, failedLoginAttempts);
        this.maxFailedAttempts = Math.max(1, maxFailedAttempts);
        this.passwordExpiresAt = passwordExpiresAt;
        this.sessionTimeoutMinutes = Math.max(5, sessionTimeoutMinutes);
        this.ipWhitelistEnabled = ipWhitelistEnabled;
        validate();
    }

    public static SecuritySettings createDefault() {
        return new SecuritySettings(
                false, // 2FA выключен по умолчанию
                0,     // нет неудачных попыток
                5,     // максимум 5 неудачных попыток
                Timestamp.now().plusMillis(90L * 24 * 60 * 60 * 1000), // пароль действует 90 дней
                30,    // сессия 30 минут
                false  // IP whitelist выключен
        );
    }

    public static SecuritySettings createForAdmin() {
        return new SecuritySettings(
                true,  // 2FA обязателен
                0,     // нет неудачных попыток
                3,     // максимум 3 неудачных попытки
                Timestamp.now().plusMillis(30L * 24 * 60 * 60 * 1000), // пароль действует 30 дней
                15,    // сессия 15 минут
                true   // IP whitelist включен
        );
    }

    public static SecuritySettings createForSuperAdmin() {
        return new SecuritySettings(
                true,  // 2FA обязателен
                0,     // нет неудачных попыток
                3,     // максимум 3 неудачных попытки
                Timestamp.now().plusMillis(30L * 24 * 60 * 60 * 1000), // пароль действует 30 дней
                10,    // сессия 10 минут
                true   // IP whitelist включен
        );
    }

    public static SecuritySettings of(boolean twoFactorEnabled, int failedLoginAttempts, int maxFailedAttempts,
                                      Timestamp passwordExpiresAt, int sessionTimeoutMinutes, boolean ipWhitelistEnabled) {
        return new SecuritySettings(twoFactorEnabled, failedLoginAttempts, maxFailedAttempts,
                passwordExpiresAt, sessionTimeoutMinutes, ipWhitelistEnabled);
    }

    @Override
    protected void validate() {
        if (passwordExpiresAt == null) {
            throw new IllegalArgumentException("Password expiry date cannot be null");
        }
        if (maxFailedAttempts < 1 || maxFailedAttempts > 10) {
            throw new IllegalArgumentException("Max failed attempts must be between 1 and 10");
        }
        if (sessionTimeoutMinutes < 5 || sessionTimeoutMinutes > 480) { // 5 минут - 8 часов
            throw new IllegalArgumentException("Session timeout must be between 5 and 480 minutes");
        }
    }

    public boolean isAccountLocked() {
        return failedLoginAttempts >= maxFailedAttempts;
    }

    public SecuritySettings incrementFailedAttempts() {
        return new SecuritySettings(twoFactorEnabled, failedLoginAttempts + 1, maxFailedAttempts,
                passwordExpiresAt, sessionTimeoutMinutes, ipWhitelistEnabled);
    }

    public SecuritySettings resetFailedAttempts() {
        return new SecuritySettings(twoFactorEnabled, 0, maxFailedAttempts,
                passwordExpiresAt, sessionTimeoutMinutes, ipWhitelistEnabled);
    }


    public boolean isPasswordExpired() {
        return passwordExpiresAt.isPast();
    }

    public boolean mustChangePasswordOnNextLogin() {
        return isPasswordExpired();
    }

    public SecuritySettings updatePasswordExpiry(Timestamp newExpiryDate) {
        return new SecuritySettings(twoFactorEnabled, failedLoginAttempts, maxFailedAttempts,
                newExpiryDate, sessionTimeoutMinutes, ipWhitelistEnabled);
    }

    public SecuritySettings updateTwoFactor(boolean enabled) {
        return new SecuritySettings(enabled, failedLoginAttempts, maxFailedAttempts,
                passwordExpiresAt, sessionTimeoutMinutes, ipWhitelistEnabled);
    }

    public SecuritySettings updateIpWhitelist(boolean enabled) {
        return new SecuritySettings(twoFactorEnabled, failedLoginAttempts, maxFailedAttempts,
                passwordExpiresAt, sessionTimeoutMinutes, enabled);
    }

    public SecuritySettings updateSessionTimeout(int timeoutMinutes) {
        return new SecuritySettings(twoFactorEnabled, failedLoginAttempts, maxFailedAttempts,
                passwordExpiresAt, timeoutMinutes, ipWhitelistEnabled);
    }

    public SecuritySettings updateMaxFailedAttempts(int maxAttempts) {
        return new SecuritySettings(twoFactorEnabled, failedLoginAttempts, maxAttempts,
                passwordExpiresAt, sessionTimeoutMinutes, ipWhitelistEnabled);
    }

    public int getRemainingAttempts() {
        return Math.max(0, maxFailedAttempts - failedLoginAttempts);
    }

    public boolean isSessionActive(Timestamp lastActivity) {
        if (lastActivity == null) {
            return false;
        }
        long sessionTimeoutMillis = sessionTimeoutMinutes * 60 * 1000L;
        return (Timestamp.now().getEpochMillis() - lastActivity.getEpochMillis()) < sessionTimeoutMillis;
    }

    public boolean isNearLockout() {
        return failedLoginAttempts >= (maxFailedAttempts - 1);
    }

    public boolean isStrictSecurityMode() {
        return twoFactorEnabled && ipWhitelistEnabled && sessionTimeoutMinutes <= 15;
    }

    public int getSecurityScore() {
        int score = 0;
        if (twoFactorEnabled) score += 40;
        if (ipWhitelistEnabled) score += 30;
        if (sessionTimeoutMinutes <= 15) score += 20;
        if (maxFailedAttempts <= 3) score += 10;
        return score;
    }

    public boolean isHighSecurity() {
        return getSecurityScore() >= 70;
    }

    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public int getMaxFailedAttempts() { return maxFailedAttempts; }
    public Timestamp getPasswordExpiresAt() { return passwordExpiresAt; }
    public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    public boolean isIpWhitelistEnabled() { return ipWhitelistEnabled; }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{twoFactorEnabled, failedLoginAttempts, maxFailedAttempts,
                passwordExpiresAt, sessionTimeoutMinutes, ipWhitelistEnabled};
    }

    @Override
    public String toString() {
        return String.format("SecuritySettings{2FA=%s, attempts=%d/%d, timeout=%dm, score=%d}",
                twoFactorEnabled, failedLoginAttempts, maxFailedAttempts, sessionTimeoutMinutes, getSecurityScore());
    }
}