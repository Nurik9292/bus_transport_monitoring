package tm.ugur.ugur_v3.domain.routeManagement.enums;

import lombok.Getter;

@Getter
public enum ScheduleType {
    FIXED("Фиксированное", "Строгое расписание с фиксированными временами отправления"),
    FLEXIBLE("Гибкое", "Гибкое расписание с возможностью корректировок"),
    FREQUENCY_BASED("По частоте", "Расписание основанное на интервалах движения"),
    ON_DEMAND("По требованию", "Движение по запросу пассажиров");

    private final String displayName;
    private final String description;

    ScheduleType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public boolean allowsFlexibility() {
        return this == FLEXIBLE || this == FREQUENCY_BASED;
    }

    public boolean requiresExactTiming() {
        return this == FIXED;
    }

    @Override
    public String toString() {
        return displayName;
    }
}