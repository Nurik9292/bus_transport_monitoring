package tm.ugur.ugur_v3.domain.staffManagement.exceptions;

import tm.ugur.ugur_v3.domain.shared.exceptions.DomainException;

public final class StaffNotFoundException extends DomainException {
    public StaffNotFoundException(String staffId) {
        super("Staff not found: " + staffId, "STAFF_NOT_FOUND");
    }
}