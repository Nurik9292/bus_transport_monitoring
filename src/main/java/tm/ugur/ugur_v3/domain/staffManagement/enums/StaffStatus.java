package tm.ugur.ugur_v3.domain.staffManagement.enums;

public enum StaffStatus {
    ACTIVE("ACTIVE", "Активный"),
    INACTIVE("INACTIVE", "Неактивный"),
    SUSPENDED("SUSPENDED", "Заблокирован"),
    PENDING_VERIFICATION("PENDING_VERIFICATION", "Ожидает подтверждения");

    private final String code;
    private final String displayName;

    StaffStatus(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean canLogin() {
        return this == ACTIVE || this == PENDING_VERIFICATION;
    }

    public boolean needsVerification() {
        return this == PENDING_VERIFICATION;
    }
}
