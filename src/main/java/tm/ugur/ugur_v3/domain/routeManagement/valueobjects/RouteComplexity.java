package tm.ugur.ugur_v3.domain.routeManagement.valueobjects;

import lombok.Getter;

@Getter
public enum RouteComplexity {
    LOW(1, "Simple route with few stops"),
    MEDIUM(2, "Moderate complexity"),
    HIGH(3, "Complex route with many stops and connections");

    private final int level;
    private final String description;

    RouteComplexity(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public boolean isHigherThan(RouteComplexity other) {
        return this.level > other.level;
    }

    public boolean isLowerThan(RouteComplexity other) {
        return this.level < other.level;
    }
}