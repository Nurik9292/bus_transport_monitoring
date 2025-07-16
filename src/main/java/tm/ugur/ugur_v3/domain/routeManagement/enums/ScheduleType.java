package tm.ugur.ugur_v3.domain.routeManagement.enums;

import lombok.Getter;

@Getter
public enum ScheduleType {
    FIXED("Fixed Schedule", "Traditional fixed timetable"),
    FLEXIBLE("Flexible Schedule", "Allows dynamic adjustments"),
    FREQUENCY_BASED("Frequency Based", "Based on headway rather than fixed times"),
    DEMAND_RESPONSIVE("Demand Responsive", "Adjusts based on passenger demand"),
    HYBRID("Hybrid", "Combination of fixed and flexible elements");

    private final String displayName;
    private final String description;

    ScheduleType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public boolean allowsDynamicChanges() {
        return this != FIXED;
    }

    public boolean isFixedTimetable() {
        return this == FIXED || this == HYBRID;
    }

    public boolean isFrequencyBased() {
        return this == FREQUENCY_BASED || this == DEMAND_RESPONSIVE;
    }
}
