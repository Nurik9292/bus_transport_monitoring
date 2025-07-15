package tm.ugur.ugur_v3.domain.vehicleManagement.exceptions;

import tm.ugur.ugur_v3.domain.shared.exceptions.DomainException;

public class VehicleStatusValidationException extends DomainException {

    public VehicleStatusValidationException(String message) {
        super(message, "INVALID_STATUS_TRANSITION");
    }
}
