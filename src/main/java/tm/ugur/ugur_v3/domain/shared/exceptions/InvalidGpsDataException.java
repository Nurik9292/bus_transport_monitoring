package tm.ugur.ugur_v3.domain.shared.exceptions;

public class InvalidGpsDataException extends DomainException {
    public InvalidGpsDataException(String message) {
        super(message, "INVALID_GPS_DATA");
    }
}