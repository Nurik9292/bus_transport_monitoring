package tm.ugur.ugur_v3.domain.staffManagement.valueobjects;

import tm.ugur.ugur_v3.domain.shared.exceptions.InvalidEntityIdException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.EntityId;

import java.util.UUID;
import java.util.regex.Pattern;

public final class StaffId extends EntityId {

    private static final Pattern VALID_STAFF_ID = Pattern.compile("^STAFF_[A-Z0-9]{12}$");

    private StaffId(String value) {
        super(value);
    }

    public static StaffId of(String value) {
        return new StaffId(value);
    }

    public static StaffId generate() {
        return new StaffId("STAFF_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
    }


    public boolean isSuperAdmin() {
        return getValue().startsWith("STAFF_SUPERADMIN");
    }

    @Override
    protected void validate() {
        super.validate();
        if (!VALID_STAFF_ID.matcher(getValue()).matches()) {
            throw new InvalidEntityIdException("Invalid staff ID format: " + getValue());
        }
    }
}