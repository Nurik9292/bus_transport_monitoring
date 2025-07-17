
package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.enums.RouteDirection;
import tm.ugur.ugur_v3.domain.routeManagement.enums.RouteType;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;

import java.util.Map;

@Getter
public final class RouteCreatedEvent extends BaseRouteEvent {

    private final String routeName;
    private final RouteType routeType;
    private final RouteDirection direction;
    private final boolean isCircular;
    private final String createdBy;

    private RouteCreatedEvent(RouteId routeId, String routeName, RouteType routeType,
                              RouteDirection direction, boolean isCircular, String createdBy,
                              String correlationId, Map<String, Object> metadata) {
        super("RouteCreated", routeId, correlationId, metadata);
        this.routeName = routeName;
        this.routeType = routeType;
        this.direction = direction;
        this.isCircular = isCircular;
        this.createdBy = createdBy;
    }

    public static RouteCreatedEvent of(RouteId routeId, String routeName, RouteType routeType,
                                       RouteDirection direction, boolean isCircular, String createdBy,
                                       String correlationId) {
        return new RouteCreatedEvent(routeId, routeName, routeType, direction, isCircular,
                createdBy, correlationId, null);
    }

    public static RouteCreatedEvent of(RouteId routeId, String routeName, RouteType routeType,
                                       RouteDirection direction, boolean isCircular, String createdBy,
                                       String correlationId, Map<String, Object> metadata) {
        return new RouteCreatedEvent(routeId, routeName, routeType, direction, isCircular,
                createdBy, correlationId, metadata);
    }

    @Override
    public String toString() {
        return String.format("RouteCreatedEvent{routeId=%s, name='%s', type=%s, direction=%s, circular=%s, by='%s'}",
                routeId, routeName, routeType, direction, isCircular, createdBy);
    }
}