package tm.ugur.ugur_v3.domain.staffManagement.exceptions;

import tm.ugur.ugur_v3.domain.shared.exceptions.DomainException;

public final class InsufficientPermissionsException extends DomainException {
    public InsufficientPermissionsException(String message) {
        super(message, "INSUFFICIENT_PERMISSIONS");
    }
}
