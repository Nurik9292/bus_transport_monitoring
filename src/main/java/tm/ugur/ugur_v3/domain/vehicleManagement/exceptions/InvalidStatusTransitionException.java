package tm.ugur.ugur_v3.domain.vehicleManagement.exceptions;

import tm.ugur.ugur_v3.domain.shared.exceptions.DomainException;

public class InvalidStatusTransitionException extends DomainException {

    public InvalidStatusTransitionException(String message) {
        super(message, "INVALID_STATUS_TRANSITION");
    }
}
