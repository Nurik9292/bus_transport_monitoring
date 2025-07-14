package tm.ugur.ugur_v3.domain.shared.exceptions;

public final class InvalidEntityIdException extends DomainException {

    public InvalidEntityIdException(String message) {
        super(message, "INVALID_ENTITY_ID");
    }

    public InvalidEntityIdException(String message, java.util.Map<String, Object> context) {
        super(message, "INVALID_ENTITY_ID", context);
    }
}