package tm.ugur.ugur_v3.domain.staffManagement.enums;

import tm.ugur.ugur_v3.domain.staffManagement.valueobjects.StaffId;

public enum StaffRole {

    SUPER_ADMIN("SUPER_ADMIN", 1000, "Супер администратор"),
    ADMIN("ADMIN", 500, "Администратор"),
    OPERATOR("OPERATOR", 100, "Оператор"),
    VIEWER("VIEWER", 10, "Наблюдатель");

    private final String code;
    private final int permissionLevel;
    private final String displayName;

    StaffRole(String code, int permissionLevel, String displayName) {
        this.code = code;
        this.permissionLevel = permissionLevel;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canManageStaff() {
        return this.permissionLevel >= ADMIN.permissionLevel;
    }

    public boolean canViewReports() {
        return this.permissionLevel >= VIEWER.permissionLevel;
    }

    public boolean canManageVehicles() {
        return this.permissionLevel >= OPERATOR.permissionLevel;
    }

    public boolean canBeUpdatedBy(StaffId updatedBy) {
        return updatedBy.isSuperAdmin() || this.permissionLevel < ADMIN.permissionLevel;
    }

    public boolean hasHigherPrivilegesThan(StaffRole other) {
        return this.permissionLevel > other.permissionLevel;
    }
}
