package tm.ugur.ugur_v3.domain.staffManagement.events;

import tm.ugur.ugur_v3.domain.shared.events.AbstractDomainEvent;
import tm.ugur.ugur_v3.domain.staffManagement.valueobjects.Email;
import tm.ugur.ugur_v3.domain.staffManagement.valueobjects.StaffId;
import tm.ugur.ugur_v3.domain.staffManagement.valueobjects.StaffName;

import java.time.LocalDateTime;
import java.util.Objects;

public final class StaffProfileUpdatedEvent extends AbstractDomainEvent {
    private final StaffId staffId;
    private final StaffName oldName;
    private final StaffName newName;
    private final Email oldEmail;
    private final Email newEmail;
    private final StaffId updatedBy;

    public StaffProfileUpdatedEvent(StaffId staffId, StaffName oldName, StaffName newName,
                                    Email oldEmail, Email newEmail, StaffId updatedBy) {
        super();
        this.staffId = Objects.requireNonNull(staffId);
        this.oldName = oldName;
        this.newName = newName;
        this.oldEmail = oldEmail;
        this.newEmail = newEmail;
        this.updatedBy = Objects.requireNonNull(updatedBy);
    }

    @Override
    public String getEventType() {
        return "StaffProfileUpdated";
    }

    @Override
    public String getAggregateId() {
        return staffId.getValue();
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return LocalDateTime.now();
    }

    public StaffId getStaffId() { return staffId; }
    public StaffName getOldName() { return oldName; }
    public StaffName getNewName() { return newName; }
    public Email getOldEmail() { return oldEmail; }
    public Email getNewEmail() { return newEmail; }
    public StaffId getUpdatedBy() { return updatedBy; }
}