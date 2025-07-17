package tm.ugur.ugur_v3.domain.routeManagement.enums;

import lombok.Getter;

@Getter
public enum ScheduleAdherenceLevel {
    EXCELLENT("Отличный", 95.0, 100.0, "🟢", "Превосходное соблюдение расписания"),
    GOOD("Хороший", 85.0, 94.9, "🟡", "Хорошее соблюдение расписания"),
    SATISFACTORY("Удовлетворительный", 75.0, 84.9, "🟠", "Удовлетворительное соблюдение"),
    POOR("Плохой", 60.0, 74.9, "🔴", "Плохое соблюдение расписания"),
    CRITICAL("Критический", 0.0, 59.9, "🚨", "Критически низкое соблюдение");

    private final String displayName;
    private final double minPercentage;
    private final double maxPercentage;
    private final String indicator;
    private final String description;

    ScheduleAdherenceLevel(String displayName, double minPercentage, double maxPercentage,
                           String indicator, String description) {
        this.displayName = displayName;
        this.minPercentage = minPercentage;
        this.maxPercentage = maxPercentage;
        this.indicator = indicator;
        this.description = description;
    }

    public static ScheduleAdherenceLevel fromPercentage(double percentage) {
        if (percentage >= 95.0) return EXCELLENT;
        if (percentage >= 85.0) return GOOD;
        if (percentage >= 75.0) return SATISFACTORY;
        if (percentage >= 60.0) return POOR;
        return CRITICAL;
    }

    public boolean requiresImmediateAction() {
        return this == POOR || this == CRITICAL;
    }

    public boolean requiresMonitoring() {
        return this == SATISFACTORY || this == POOR || this == CRITICAL;
    }

    public boolean isAcceptable() {
        return this == EXCELLENT || this == GOOD;
    }

    public int getRecommendedCheckIntervalHours() {
        return switch (this) {
            case EXCELLENT -> 24;    // Раз в день
            case GOOD -> 12;         // Дважды в день
            case SATISFACTORY -> 6;  // Каждые 6 часов
            case POOR -> 2;          // Каждые 2 часа
            case CRITICAL -> 1;      // Каждый час
        };
    }

    @Override
    public String toString() {
        return indicator + " " + displayName + String.format(" (%.1f-%.1f%%)", minPercentage, maxPercentage);
    }
}