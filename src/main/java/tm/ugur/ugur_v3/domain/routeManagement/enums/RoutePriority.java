package tm.ugur.ugur_v3.domain.routeManagement.enums;

import lombok.Getter;

@Getter
public enum RoutePriority {
    CRITICAL("Критический", 1, "Основные транспортные артерии", true, 95.0),
    HIGH("Высокий", 2, "Важные соединения", true, 90.0),
    MEDIUM("Средний", 3, "Стандартные маршруты", false, 85.0),
    LOW("Низкий", 4, "Дополнительные маршруты", false, 80.0),
    MINIMAL("Минимальный", 5, "Вспомогательные маршруты", false, 75.0);

    private final String displayName;
    private final int level;
    private final String description;
    private final boolean requiresConstantMonitoring;
    private final double requiredOnTimePerformance;

    RoutePriority(String displayName, int level, String description,
                  boolean monitoring, double performance) {
        this.displayName = displayName;
        this.level = level;
        this.description = description;
        this.requiresConstantMonitoring = monitoring;
        this.requiredOnTimePerformance = performance;
    }

    public boolean isHigherThan(RoutePriority other) {
        return this.level < other.level; // Меньший номер = выше приоритет
    }

    public boolean isLowerThan(RoutePriority other) {
        return this.level > other.level;
    }

    public boolean requiresImmediateResponse() {
        return this == CRITICAL;
    }

    public int getMaxAllowedDelayMinutes() {
        return switch (this) {
            case CRITICAL -> 2;
            case HIGH -> 5;
            case MEDIUM -> 10;
            case LOW -> 15;
            case MINIMAL -> 20;
        };
    }

    public int getMaintenanceIntervalDays() {
        return switch (this) {
            case CRITICAL -> 7;
            case HIGH -> 14;
            case MEDIUM -> 30;
            case LOW -> 60;
            case MINIMAL -> 90;
        };
    }

    @Override
    public String toString() {
        return displayName + " (P" + level + ")";
    }
}
