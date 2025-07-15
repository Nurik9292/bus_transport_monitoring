package tm.ugur.ugur_v3.domain.vehicleManagement.exceptions;

import tm.ugur.ugur_v3.domain.shared.exceptions.DomainException;

public class InvalidGpsDataException extends DomainException {

    public InvalidGpsDataException(String message) {
        super(message, "INVALID_GPS_DATA");
    }
}
