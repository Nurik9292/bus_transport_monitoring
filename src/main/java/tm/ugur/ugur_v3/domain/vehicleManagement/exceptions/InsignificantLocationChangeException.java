package tm.ugur.ugur_v3.domain.vehicleManagement.exceptions;

import tm.ugur.ugur_v3.domain.shared.exceptions.DomainException;

public class InsignificantLocationChangeException extends DomainException {

    public InsignificantLocationChangeException(String message) {
        super(message, "INSIGNIFICANT_LOCATION_CHANGE");
    }
}
