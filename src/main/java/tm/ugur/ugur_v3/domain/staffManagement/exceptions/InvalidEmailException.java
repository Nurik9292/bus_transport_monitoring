package tm.ugur.ugur_v3.domain.staffManagement.exceptions;

import tm.ugur.ugur_v3.domain.shared.exceptions.DomainException;

public final class InvalidEmailException extends DomainException {
    public InvalidEmailException(String message) {
        super(message, "INVALID_EMAIL");
    }
}
