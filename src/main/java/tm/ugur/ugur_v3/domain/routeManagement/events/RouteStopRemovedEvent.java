
package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;

import java.util.Map;

@Getter
public final class RouteStopRemovedEvent extends BaseRouteEvent {

    private final StopId stopId;
    private final int previousPosition;
    private final String stopName;
    private final String reason;

    private RouteStopRemovedEvent(RouteId routeId, StopId stopId, int previousPosition,
                                  String stopName, String reason, String correlationId,
                                  Map<String, Object> metadata) {
        super("RouteStopRemoved", routeId, correlationId, metadata);
        this.stopId = stopId;
        this.previousPosition = previousPosition;
        this.stopName = stopName;
        this.reason = reason;
    }

    public static RouteStopRemovedEvent of(RouteId routeId, StopId stopId, int previousPosition,
                                           String reason) {
        return new RouteStopRemovedEvent(routeId, stopId, previousPosition, null, reason, null, null);
    }

    public static RouteStopRemovedEvent of(RouteId routeId, StopId stopId, int previousPosition,
                                           String stopName, String reason) {
        return new RouteStopRemovedEvent(routeId, stopId, previousPosition, stopName, reason, null, null);
    }

    @Override
    public String toString() {
        return String.format("RouteStopRemovedEvent{routeId=%s, stopId=%s, position=%d, reason='%s'}",
                routeId, stopId, previousPosition, reason);
    }
}