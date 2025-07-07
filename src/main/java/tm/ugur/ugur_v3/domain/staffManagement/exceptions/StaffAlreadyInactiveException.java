package tm.ugur.ugur_v3.domain.staffManagement.exceptions;

import tm.ugur.ugur_v3.domain.shared.exceptions.DomainException;

public final class StaffAlreadyInactiveException extends DomainException {
    public StaffAlreadyInactiveException(String message) {
        super(message, "STAFF_ALREADY_INACTIVE");
    }
}
