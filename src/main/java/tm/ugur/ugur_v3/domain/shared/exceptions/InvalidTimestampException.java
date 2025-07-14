package tm.ugur.ugur_v3.domain.shared.exceptions;

public final class InvalidTimestampException extends DomainException {

    public InvalidTimestampException(String message) {
        super(message, "INVALID_TIMESTAMP");
    }

    public InvalidTimestampException(String message, java.util.Map<String, Object> context) {
        super(message, "INVALID_TIMESTAMP", context);
    }
}