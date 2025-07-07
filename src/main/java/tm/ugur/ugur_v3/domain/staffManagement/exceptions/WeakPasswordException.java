package tm.ugur.ugur_v3.domain.staffManagement.exceptions;

import tm.ugur.ugur_v3.domain.shared.exceptions.DomainException;

public final class WeakPasswordException extends DomainException {
    public WeakPasswordException(String message) {
        super(message, "WEAK_PASSWORD");
    }
}