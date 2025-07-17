package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;

import java.util.Map;

@Getter
public final class RoutePerformanceUpdatedEvent extends BaseRouteEvent {

    private final double onTimePerformance;
    private final int dailyRidership;
    private final double averageSpeed;
    private final double previousOnTimePerformance;
    private final int previousDailyRidership;
    private final double improvementPercentage;

    private RoutePerformanceUpdatedEvent(RouteId routeId, double onTimePerformance, int dailyRidership,
                                         double averageSpeed, double previousOnTimePerformance,
                                         int previousDailyRidership, String correlationId,
                                         Map<String, Object> metadata) {
        super("RoutePerformanceUpdated", routeId, correlationId, metadata);
        this.onTimePerformance = onTimePerformance;
        this.dailyRidership = dailyRidership;
        this.averageSpeed = averageSpeed;
        this.previousOnTimePerformance = previousOnTimePerformance;
        this.previousDailyRidership = previousDailyRidership;
        this.improvementPercentage = calculateImprovement();
    }

    private double calculateImprovement() {
        if (previousOnTimePerformance == 0) return 0;
        return ((onTimePerformance - previousOnTimePerformance) / previousOnTimePerformance) * 100;
    }

    public static RoutePerformanceUpdatedEvent of(RouteId routeId, double onTimePerformance, int dailyRidership) {
        return new RoutePerformanceUpdatedEvent(routeId, onTimePerformance, dailyRidership,
                0, 0, 0, null, null);
    }

    public static RoutePerformanceUpdatedEvent of(RouteId routeId, double onTimePerformance, int dailyRidership,
                                                  double previousOnTime, int previousRidership) {
        return new RoutePerformanceUpdatedEvent(routeId, onTimePerformance, dailyRidership, 0,
                previousOnTime, previousRidership, null, null);
    }

    public boolean isImprovement() { return improvementPercentage > 0; }
    public boolean isSignificantChange() { return Math.abs(improvementPercentage) > 5.0; }

    @Override
    public String toString() {
        return String.format("RoutePerformanceUpdatedEvent{routeId=%s, onTime=%.1f%%, ridership=%d, change=%.1f%%}",
                routeId, onTimePerformance, dailyRidership, improvementPercentage);
    }
}