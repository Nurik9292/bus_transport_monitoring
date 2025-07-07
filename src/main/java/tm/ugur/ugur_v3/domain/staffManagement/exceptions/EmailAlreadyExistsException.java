package tm.ugur.ugur_v3.domain.staffManagement.exceptions;

import tm.ugur.ugur_v3.domain.shared.exceptions.DomainException;

public final class EmailAlreadyExistsException extends DomainException {
    public EmailAlreadyExistsException(String email) {
        super("Email already exists: " + email, "EMAIL_ALREADY_EXISTS");
    }
}