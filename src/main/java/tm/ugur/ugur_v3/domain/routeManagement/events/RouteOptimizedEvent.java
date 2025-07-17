package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.geospatial.valueobjects.Distance;
import tm.ugur.ugur_v3.domain.routeManagement.enums.RouteComplexity;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.EstimatedDuration;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;

import java.util.Map;

@Getter
public final class RouteOptimizedEvent extends BaseRouteEvent {

    private final Distance oldDistance;
    private final Distance newDistance;
    private final EstimatedDuration oldDuration;
    private final EstimatedDuration newDuration;
    private final RouteComplexity oldComplexity;
    private final RouteComplexity newComplexity;
    private final String optimizationType;
    private final double improvementPercentage;

    private RouteOptimizedEvent(RouteId routeId, Distance oldDistance, Distance newDistance,
                                EstimatedDuration oldDuration, EstimatedDuration newDuration,
                                RouteComplexity oldComplexity, RouteComplexity newComplexity,
                                String optimizationType, String correlationId, Map<String, Object> metadata) {
        super("RouteOptimized", routeId, correlationId, metadata);
        this.oldDistance = oldDistance;
        this.newDistance = newDistance;
        this.oldDuration = oldDuration;
        this.newDuration = newDuration;
        this.oldComplexity = oldComplexity;
        this.newComplexity = newComplexity;
        this.optimizationType = optimizationType;
        this.improvementPercentage = calculateImprovement();
    }

    private double calculateImprovement() {
        double oldValue = oldDistance.toKilometers() + (oldDuration.getHours() * 10);
        double newValue = newDistance.toKilometers() + (newDuration.getHours() * 10);
        return ((oldValue - newValue) / oldValue) * 100;
    }

    public static RouteOptimizedEvent of(RouteId routeId, Distance oldDistance, Distance newDistance,
                                         EstimatedDuration oldDuration, EstimatedDuration newDuration,
                                         String optimizationType) {
        return new RouteOptimizedEvent(routeId, oldDistance, newDistance, oldDuration, newDuration,
                null, null, optimizationType, null, null);
    }

    @Override
    public String toString() {
        return String.format("RouteOptimizedEvent{routeId=%s, type='%s', improvement=%.1f%%}",
                routeId, optimizationType, improvementPercentage);
    }
}
