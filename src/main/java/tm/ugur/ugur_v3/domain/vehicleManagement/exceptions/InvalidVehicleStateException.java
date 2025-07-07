package tm.ugur.ugur_v3.domain.vehicleManagement.exceptions;

import tm.ugur.ugur_v3.domain.shared.exceptions.DomainException;

public class InvalidVehicleStateException extends DomainException {
    public InvalidVehicleStateException(String message) {
        super(message, "INVALID_VEHICLE_STATE");
    }
}