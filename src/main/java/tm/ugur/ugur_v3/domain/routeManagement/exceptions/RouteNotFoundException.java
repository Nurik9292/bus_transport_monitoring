package tm.ugur.ugur_v3.domain.routeManagement.exceptions;

import tm.ugur.ugur_v3.domain.shared.exceptions.DomainException;

public class RouteNotFoundException extends DomainException {
    public RouteNotFoundException(String routeId) {
        super("Route not found: " + routeId, "ROUTE_NOT_FOUND");
    }
}