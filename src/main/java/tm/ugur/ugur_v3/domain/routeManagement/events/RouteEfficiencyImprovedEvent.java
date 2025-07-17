package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;

import java.util.List;
import java.util.Map;

@Getter
public final class RouteEfficiencyImprovedEvent extends BaseRouteEvent {

    private final String improvementType;
    private final double oldEfficiencyScore;
    private final double newEfficiencyScore;
    private final List<String> implementedChanges;
    private final double costSavingsPerDay;
    private final double timeSavingsPerTrip;
    private final String implementedBy;

    private RouteEfficiencyImprovedEvent(RouteId routeId, String improvementType, double oldEfficiencyScore,
                                         double newEfficiencyScore, List<String> implementedChanges,
                                         double costSavingsPerDay, double timeSavingsPerTrip, String implementedBy,
                                         String correlationId, Map<String, Object> metadata) {
        super("RouteEfficiencyImproved", routeId, correlationId, metadata);
        this.improvementType = improvementType;
        this.oldEfficiencyScore = oldEfficiencyScore;
        this.newEfficiencyScore = newEfficiencyScore;
        this.implementedChanges = implementedChanges != null ? List.copyOf(implementedChanges) : List.of();
        this.costSavingsPerDay = costSavingsPerDay;
        this.timeSavingsPerTrip = timeSavingsPerTrip;
        this.implementedBy = implementedBy;
    }

    public static RouteEfficiencyImprovedEvent of(RouteId routeId, String improvementType,
                                                  double oldEfficiencyScore, double newEfficiencyScore,
                                                  List<String> implementedChanges, String implementedBy) {
        return new RouteEfficiencyImprovedEvent(routeId, improvementType, oldEfficiencyScore, newEfficiencyScore,
                implementedChanges, 0.0, 0.0, implementedBy, null, null);
    }

    public double getImprovementPercentage() {
        if (oldEfficiencyScore == 0) return 0;
        return ((newEfficiencyScore - oldEfficiencyScore) / oldEfficiencyScore) * 100;
    }

    public boolean isSignificantImprovement() { return getImprovementPercentage() > 10.0; }
    public boolean hasCostSavings() { return costSavingsPerDay > 0; }
    public boolean hasTimeSavings() { return timeSavingsPerTrip > 0; }

    @Override
    public String toString() {
        return String.format("RouteEfficiencyImprovedEvent{routeId=%s, type='%s', improvement=%.1f%%, savings=%.2f/day}",
                routeId, improvementType, getImprovementPercentage(), costSavingsPerDay);
    }
}