package tm.ugur.ugur_v3.domain.staffManagement.exceptions;

import tm.ugur.ugur_v3.domain.shared.exceptions.DomainException;

public final class InvalidPasswordException extends DomainException {
    public InvalidPasswordException(String message) {
        super(message, "INVALID_PASSWORD");
    }
}
