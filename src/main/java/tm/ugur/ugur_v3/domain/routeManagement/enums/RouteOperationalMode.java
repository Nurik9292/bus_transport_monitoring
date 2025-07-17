package tm.ugur.ugur_v3.domain.routeManagement.enums;

import lombok.Getter;

@Getter
public enum RouteOperationalMode {
    NORMAL("Обычный", "Стандартный режим работы", 1.0, 1.0),
    PEAK_ENHANCED("Усиленный час пик", "Увеличенная частота в час пик", 0.7, 1.5),
    REDUCED("Сокращенный", "Сокращенная частота", 1.5, 0.8),
    WEEKEND("Выходной", "Режим выходного дня", 1.3, 0.9),
    HOLIDAY("Праздничный", "Праздничное расписание", 1.8, 0.6),
    EMERGENCY("Аварийный", "Экстренный режим", 2.0, 0.5),
    MAINTENANCE("Техобслуживание", "Режим технического обслуживания", 3.0, 0.3),
    WEATHER_ADJUSTED("Погодная корректировка", "Корректировка из-за погоды", 1.4, 0.9);

    private final String displayName;
    private final String description;
    private final double headwayMultiplier;
    private final double capacityMultiplier;

    RouteOperationalMode(String displayName, String description,
                         double headwayMultiplier, double capacityMultiplier) {
        this.displayName = displayName;
        this.description = description;
        this.headwayMultiplier = headwayMultiplier;
        this.capacityMultiplier = capacityMultiplier;
    }

    public boolean isEnhancedService() {
        return headwayMultiplier < 1.0;
    }

    public boolean isReducedService() {
        return headwayMultiplier > 1.0;
    }

    public boolean isEmergencyMode() {
        return this == EMERGENCY || this == MAINTENANCE;
    }

    public boolean allowsScheduleDeviation() {
        return this == EMERGENCY || this == WEATHER_ADJUSTED || this == MAINTENANCE;
    }

    public int adjustHeadway(int baseHeadwayMinutes) {
        return (int) Math.round(baseHeadwayMinutes * headwayMultiplier);
    }

    public int adjustCapacity(int baseCapacity) {
        return (int) Math.round(baseCapacity * capacityMultiplier);
    }

    public boolean requiresApproval() {
        return this == EMERGENCY || this == MAINTENANCE;
    }

    @Override
    public String toString() {
        return displayName;
    }
}