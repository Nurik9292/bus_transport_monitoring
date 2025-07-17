package tm.ugur.ugur_v3.domain.routeManagement.enums;

import lombok.Getter;

@Getter
public enum ServiceFrequency {
    VERY_HIGH("Очень высокая", 3, 5, "Каждые 3-5 минут"),
    HIGH("Высокая", 5, 10, "Каждые 5-10 минут"),
    MEDIUM("Средняя", 10, 20, "Каждые 10-20 минут"),
    LOW("Низкая", 20, 40, "Каждые 20-40 минут"),
    VERY_LOW("Очень низкая", 40, 90, "Каждые 40-90 минут"),
    HOURLY("Каждый час", 60, 60, "Каждый час"),
    PEAK_ONLY("Только час пик", 15, 30, "Только в часы пик"),
    LIMITED("Ограниченная", 90, 180, "Ограниченное расписание");

    private final String displayName;
    private final int minHeadwayMinutes;
    private final int maxHeadwayMinutes;
    private final String description;

    ServiceFrequency(String displayName, int minHeadway, int maxHeadway, String description) {
        this.displayName = displayName;
        this.minHeadwayMinutes = minHeadway;
        this.maxHeadwayMinutes = maxHeadway;
        this.description = description;
    }

    public int getDefaultHeadwayMinutes() {
        return (minHeadwayMinutes + maxHeadwayMinutes) / 2;
    }

    public boolean isHighFrequency() {
        return maxHeadwayMinutes <= 10;
    }

    public boolean isLowFrequency() {
        return minHeadwayMinutes >= 40;
    }

    public int getTripsPerHour() {
        return 60 / getDefaultHeadwayMinutes();
    }

    public int getEstimatedDailyTrips(int operatingHours) {
        return (operatingHours * 60) / getDefaultHeadwayMinutes();
    }

    public boolean isSuitableForRouteType(RouteType routeType) {
        return switch (routeType) {
            case EXPRESS, FEEDER -> this == HIGH || this == MEDIUM;
            case REGULAR, TOURIST -> this == MEDIUM || this == LOW;
            case NIGHT -> this == LOW || this == VERY_LOW || this == HOURLY;
            case SHUTTLE -> this == VERY_HIGH || this == HIGH;
            case INTERCITY -> this == LOW || this == VERY_LOW || this == LIMITED;
            case SCHOOL -> this == PEAK_ONLY || this == LIMITED;
        };
    }

    @Override
    public String toString() {
        return displayName + " (" + description + ")";
    }
}