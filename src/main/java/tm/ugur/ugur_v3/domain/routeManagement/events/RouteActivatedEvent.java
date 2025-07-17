package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.OperatingHours;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;

import java.util.Map;

@Getter
public final class RouteActivatedEvent extends BaseRouteEvent {

    private final OperatingHours operatingHours;
    private final String activatedBy;

    private RouteActivatedEvent(RouteId routeId, OperatingHours operatingHours, String activatedBy,
                                String correlationId, Map<String, Object> metadata) {
        super("RouteActivated", routeId, correlationId, metadata);
        this.operatingHours = operatingHours;
        this.activatedBy = activatedBy;
    }

    public static RouteActivatedEvent of(RouteId routeId, OperatingHours operatingHours) {
        return new RouteActivatedEvent(routeId, operatingHours, "SYSTEM", null, null);
    }

    public static RouteActivatedEvent of(RouteId routeId, OperatingHours operatingHours, String activatedBy) {
        return new RouteActivatedEvent(routeId, operatingHours, activatedBy, null, null);
    }

    @Override
    public String toString() {
        return String.format("RouteActivatedEvent{routeId=%s, operatingHours=%s, by='%s'}",
                routeId, operatingHours, activatedBy);
    }
}