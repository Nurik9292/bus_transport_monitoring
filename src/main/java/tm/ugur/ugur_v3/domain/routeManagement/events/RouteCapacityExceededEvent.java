package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;

import java.time.LocalTime;
import java.util.Map;

@Getter
public final class RouteCapacityExceededEvent extends BaseRouteEvent {

    private final StopId stopId;
    private final LocalTime timeOfOccurrence;
    private final int expectedCapacity;
    private final int actualPassengerCount;
    private final double overloadPercentage;
    private final boolean passengersLeftBehind;
    private final String reportingVehicle;

    private RouteCapacityExceededEvent(RouteId routeId, StopId stopId, LocalTime timeOfOccurrence,
                                       int expectedCapacity, int actualPassengerCount,
                                       boolean passengersLeftBehind, String reportingVehicle,
                                       String correlationId, Map<String, Object> metadata) {
        super("RouteCapacityExceeded", routeId, correlationId, metadata);
        this.stopId = stopId;
        this.timeOfOccurrence = timeOfOccurrence;
        this.expectedCapacity = expectedCapacity;
        this.actualPassengerCount = actualPassengerCount;
        this.overloadPercentage = calculateOverloadPercentage();
        this.passengersLeftBehind = passengersLeftBehind;
        this.reportingVehicle = reportingVehicle;
    }

    private double calculateOverloadPercentage() {
        if (expectedCapacity == 0) return 0;
        return ((double) (actualPassengerCount - expectedCapacity) / expectedCapacity) * 100;
    }

    public static RouteCapacityExceededEvent of(RouteId routeId, StopId stopId, int expectedCapacity,
                                                int actualPassengerCount, boolean passengersLeftBehind,
                                                String reportingVehicle) {
        return new RouteCapacityExceededEvent(routeId, stopId, LocalTime.now(), expectedCapacity,
                actualPassengerCount, passengersLeftBehind, reportingVehicle, null, null);
    }

    public boolean isSevereOverload() { return overloadPercentage > 50.0; }
    public boolean isMinorOverload() { return overloadPercentage <= 20.0; }
    public boolean requiresImmediateAction() { return passengersLeftBehind || isSevereOverload(); }

    @Override
    public String toString() {
        return String.format("RouteCapacityExceededEvent{routeId=%s, stopId=%s, overload=%.1f%%, leftBehind=%s}",
                routeId, stopId, overloadPercentage, passengersLeftBehind);
    }
}