package tm.ugur.ugur_v3.domain.routeManagement.enums;

import lombok.Getter;

@Getter
public enum RouteMaintenanceStatus {
    OPTIMAL("Оптимальное", "Не требует обслуживания", 0, 90),
    GOOD("Хорошее", "Плановое обслуживание", 30, 60),
    ATTENTION_NEEDED("Требует внимания", "Нужно плановое обслуживание", 14, 30),
    MAINTENANCE_DUE("Обслуживание просрочено", "Просроченное обслуживание", 7, 14),
    URGENT("Срочное", "Требует немедленного вмешательства", 1, 7),
    CRITICAL("Критическое", "Риск прекращения работы", 0, 1);

    private final String displayName;
    private final String description;
    private final int minDaysToMaintenance;
    private final int maxDaysToMaintenance;

    RouteMaintenanceStatus(String displayName, String description,
                           int minDays, int maxDays) {
        this.displayName = displayName;
        this.description = description;
        this.minDaysToMaintenance = minDays;
        this.maxDaysToMaintenance = maxDays;
    }

    public static RouteMaintenanceStatus fromDaysToMaintenance(int daysToMaintenance) {
        if (daysToMaintenance > 90) return OPTIMAL;
        if (daysToMaintenance > 60) return GOOD;
        if (daysToMaintenance > 30) return ATTENTION_NEEDED;
        if (daysToMaintenance > 14) return MAINTENANCE_DUE;
        if (daysToMaintenance > 7) return URGENT;
        return CRITICAL;
    }

    public boolean requiresImmediateAction() {
        return this == URGENT || this == CRITICAL;
    }

    public boolean canOperateNormally() {
        return this == OPTIMAL || this == GOOD || this == ATTENTION_NEEDED;
    }

    public boolean shouldScheduleMaintenance() {
        return this == ATTENTION_NEEDED || this == MAINTENANCE_DUE;
    }

    public MaintenancePriority getMaintenancePriority() {
        return switch (this) {
            case CRITICAL -> MaintenancePriority.EMERGENCY;
            case URGENT -> MaintenancePriority.HIGH;
            case MAINTENANCE_DUE -> MaintenancePriority.MEDIUM;
            case ATTENTION_NEEDED -> MaintenancePriority.LOW;
            case GOOD, OPTIMAL -> MaintenancePriority.ROUTINE;
        };
    }

    @Override
    public String toString() {
        return displayName + " (" + description + ")";
    }

    public enum MaintenancePriority {
        EMERGENCY("Экстренное", 1),
        HIGH("Высокий", 2),
        MEDIUM("Средний", 3),
        LOW("Низкий", 4),
        ROUTINE("Плановое", 5);

        private final String name;
        private final int level;

        MaintenancePriority(String name, int level) {
            this.name = name;
            this.level = level;
        }

        public boolean isHigherThan(MaintenancePriority other) {
            return this.level < other.level;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}