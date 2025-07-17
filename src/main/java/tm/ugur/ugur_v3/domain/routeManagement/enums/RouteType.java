package tm.ugur.ugur_v3.domain.routeManagement.enums;

import lombok.Getter;

@Getter
public enum RouteType {
    REGULAR("Обычный", "Стандартный городской маршрут", 30, 50, 15.0),
    EXPRESS("Экспресс", "Скоростной маршрут с ограниченными остановками", 60, 80, 25.0),
    NIGHT("Ночной", "Ночной маршрут с увеличенными интервалами", 60, 120, 20.0),
    SHUTTLE("Шаттл", "Короткий маршрут между ключевыми точками", 15, 30, 12.0),
    INTERCITY("Междугородний", "Маршрут между городами", 120, 300, 45.0),
    TOURIST("Туристический", "Экскурсионный маршрут", 45, 90, 18.0),
    SCHOOL("Школьный", "Маршрут для перевозки школьников", 30, 60, 20.0),
    FEEDER("Подвозящий", "Маршрут к транспортным узлам", 20, 40, 15.0);

    private final String displayName;
    private final String description;
    private final int minHeadwayMinutes;
    private final int maxHeadwayMinutes;
    private final double expectedAverageSpeed;

    RouteType(String displayName, String description, int minHeadway, int maxHeadway, double avgSpeed) {
        this.displayName = displayName;
        this.description = description;
        this.minHeadwayMinutes = minHeadway;
        this.maxHeadwayMinutes = maxHeadway;
        this.expectedAverageSpeed = avgSpeed;
    }

    public boolean isHighFrequency() {
        return minHeadwayMinutes <= 15;
    }

    public boolean isLongDistance() {
        return this == INTERCITY || expectedAverageSpeed > 40.0;
    }

    public boolean isSpecialPurpose() {
        return this == SCHOOL || this == TOURIST || this == SHUTTLE;
    }

    public int getRecommendedCapacity() {
        return switch (this) {
            case EXPRESS, INTERCITY -> 80;
            case REGULAR, NIGHT -> 60;
            case SHUTTLE, FEEDER -> 40;
            case SCHOOL -> 50;
            case TOURIST -> 45;
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
