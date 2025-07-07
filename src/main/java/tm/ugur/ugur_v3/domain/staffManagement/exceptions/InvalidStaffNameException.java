package tm.ugur.ugur_v3.domain.staffManagement.exceptions;

import tm.ugur.ugur_v3.domain.shared.exceptions.DomainException;

public final class InvalidStaffNameException extends DomainException {
    public InvalidStaffNameException(String message) {
        super(message, "INVALID_STAFF_NAME");
    }
}

