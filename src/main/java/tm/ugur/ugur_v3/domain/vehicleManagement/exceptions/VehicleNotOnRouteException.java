package tm.ugur.ugur_v3.domain.vehicleManagement.exceptions;

import tm.ugur.ugur_v3.domain.shared.exceptions.DomainException;

public class VehicleNotOnRouteException extends DomainException {
    public VehicleNotOnRouteException(String vehicleId, String routeId) {
        super("Vehicle " + vehicleId + " is not on route " + routeId, "VEHICLE_NOT_ON_ROUTE");
    }
}
