package tm.ugur.ugur_v3.domain.routeManagement.enums;

import lombok.Getter;

@Getter
public enum RouteComplexity {
    LOW(1, "Низкая", "Простой маршрут с небольшим количеством остановок"),
    MEDIUM(2, "Средняя", "Стандартный городской маршрут"),
    HIGH(3, "Высокая", "Сложный маршрут с большим количеством остановок или сложной геометрией");

    private final int level;
    private final String displayName;
    private final String description;

    RouteComplexity(int level, String displayName, String description) {
        this.level = level;
        this.displayName = displayName;
        this.description = description;
    }

    public boolean isHigherThan(RouteComplexity other) {
        return this.level > other.level;
    }

    public boolean isLowerThan(RouteComplexity other) {
        return this.level < other.level;
    }

    public static RouteComplexity fromLevel(int level) {
        return switch (level) {
            case 1 -> LOW;
            case 2 -> MEDIUM;
            case 3 -> HIGH;
            default -> throw new IllegalArgumentException("Invalid complexity level: " + level);
        };
    }

    @Override
    public String toString() {
        return displayName + " (" + level + "/3)";
    }
}