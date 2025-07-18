package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;

import java.util.List;


/**
 * Предложение по улучшению маршрута
 * TODO: Реализовать в Infrastructure слое при ML интеграции
 */
public record RouteImprovement(
        ImprovementType type,
        String description,
        double expectedImprovement,
        List<StopId> affectedStops,
        String rationale,
        Priority priority
) {

    public enum ImprovementType {
        ADD_STOP,
        REMOVE_STOP,
        REORDER_STOPS,
        ADJUST_FREQUENCY,
        CHANGE_VEHICLE_TYPE,
        MODIFY_SCHEDULE
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public static RouteImprovement addStop(StopId stopId, double improvement) {
        return new RouteImprovement(
                ImprovementType.ADD_STOP,
                "Add new stop to improve coverage",
                improvement,
                List.of(stopId),
                "High passenger demand detected",
                Priority.MEDIUM
        );
    }

    public boolean isHighImpact() {
        return expectedImprovement > 0.1; // 10%+ improvement
    }
}