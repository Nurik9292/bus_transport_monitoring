package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;

import java.util.Map;

@Getter
public final class RouteDeactivatedEvent extends BaseRouteEvent {

    private final String reason;
    private final String deactivatedBy;
    private final boolean isTemporary;

    private RouteDeactivatedEvent(RouteId routeId, String reason, String deactivatedBy,
                                  boolean isTemporary, String correlationId, Map<String, Object> metadata) {
        super("RouteDeactivated", routeId, correlationId, metadata);
        this.reason = reason;
        this.deactivatedBy = deactivatedBy;
        this.isTemporary = isTemporary;
    }

    public static RouteDeactivatedEvent of(RouteId routeId, String reason, String deactivatedBy) {
        return new RouteDeactivatedEvent(routeId, reason, deactivatedBy, false, null, null);
    }

    public static RouteDeactivatedEvent temporary(RouteId routeId, String reason, String deactivatedBy) {
        return new RouteDeactivatedEvent(routeId, reason, deactivatedBy, true, null, null);
    }

    @Override
    public String toString() {
        return String.format("RouteDeactivatedEvent{routeId=%s, reason='%s', temporary=%s, by='%s'}",
                routeId, reason, isTemporary, deactivatedBy);
    }
}