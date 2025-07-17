package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;

import java.util.Map;

@Getter
public final class RouteStopAddedEvent extends BaseRouteEvent {

    private final StopId stopId;
    private final int position;
    private final GeoCoordinate stopLocation;
    private final String stopName;

    private RouteStopAddedEvent(RouteId routeId, StopId stopId, int position,
                                GeoCoordinate stopLocation, String stopName,
                                String correlationId, Map<String, Object> metadata) {
        super("RouteStopAdded", routeId, correlationId, metadata);
        this.stopId = stopId;
        this.position = position;
        this.stopLocation = stopLocation;
        this.stopName = stopName;
    }

    public static RouteStopAddedEvent of(RouteId routeId, StopId stopId, int position,
                                         GeoCoordinate stopLocation, String stopName) {
        return new RouteStopAddedEvent(routeId, stopId, position, stopLocation, stopName, null, null);
    }

    public boolean isFirstStop() { return position == 0; }
    public boolean hasValidLocation() { return stopLocation != null; }
    public boolean hasStopName() { return stopName != null && !stopName.trim().isEmpty(); }

    @Override
    public String toString() {
        return String.format("RouteStopAddedEvent{routeId=%s, stopId=%s, position=%d, name='%s'}",
                routeId, stopId, position, stopName);
    }
}
