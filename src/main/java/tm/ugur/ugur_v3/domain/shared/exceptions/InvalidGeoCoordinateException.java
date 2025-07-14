package tm.ugur.ugur_v3.domain.shared.exceptions;

public final class InvalidGeoCoordinateException extends DomainException {

    public InvalidGeoCoordinateException(String message) {
        super(message, "INVALID_GEO_COORDINATE");
    }

    public InvalidGeoCoordinateException(String message, java.util.Map<String, Object> context) {
        super(message, "INVALID_GEO_COORDINATE", context);
    }
}