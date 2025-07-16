package tm.ugur.ugur_v3.domain.routeManagement.enums;

import lombok.Getter;

@Getter
public enum RoutePriority {

    CRITICAL("Critical", "Essential service route", 1, 99.5),
    HIGH("High", "High importance route", 2, 95.0),
    NORMAL("Normal", "Standard service route", 3, 90.0),
    LOW("Low", "Lower priority route", 4, 80.0);

    private final String displayName;
    private final String description;
    private final int level;
    private final double targetReliability;

    RoutePriority(String displayName, String description, int level, double targetReliability) {
        this.displayName = displayName;
        this.description = description;
        this.level = level;
        this.targetReliability = targetReliability;
    }

    public boolean isHigherThan(RoutePriority other) {
        return this.level < other.level;
    }

    public boolean isLowerThan(RoutePriority other) {
        return this.level > other.level;
    }

    public boolean isCritical() {
        return this == CRITICAL;
    }

    public double getResourceWeight() {
        return switch (this) {
            case CRITICAL -> 2.0;
            case HIGH -> 1.5;
            case NORMAL -> 1.0;
            case LOW -> 0.7;
        };
    }

    public int getMaintenancePriorityHours() {
        return switch (this) {
            case CRITICAL -> 2;
            case HIGH -> 6;
            case NORMAL -> 24;
            case LOW -> 72;
        };
    }
}