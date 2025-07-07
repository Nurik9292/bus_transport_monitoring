package tm.ugur.ugur_v3.domain.staffManagement.exceptions;

import tm.ugur.ugur_v3.domain.shared.exceptions.DomainException;

public final class InvalidRoleAssignmentException extends DomainException {
    public InvalidRoleAssignmentException(String message) {
        super(message, "INVALID_ROLE_ASSIGNMENT");
    }
}
