package tm.ugur.ugur_v3.domain.staffManagement.events;

import tm.ugur.ugur_v3.domain.shared.events.AbstractDomainEvent;
import tm.ugur.ugur_v3.domain.staffManagement.enums.StaffRole;
import tm.ugur.ugur_v3.domain.staffManagement.valueobjects.StaffId;

import java.time.LocalDateTime;
import java.util.Objects;

public final class StaffRoleChangedEvent extends AbstractDomainEvent {

    private final StaffId staffId;
    private final StaffRole oldRole;
    private final StaffRole newRole;
    private final StaffId changedBy;

    public StaffRoleChangedEvent(StaffId staffId, StaffRole oldRole, StaffRole newRole, StaffId changedBy) {
        super();
        this.staffId = Objects.requireNonNull(staffId);
        this.oldRole = Objects.requireNonNull(oldRole);
        this.newRole = Objects.requireNonNull(newRole);
        this.changedBy = Objects.requireNonNull(changedBy);
    }

    @Override
    public String getEventType() {
        return "StaffRoleChanged";
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
    public StaffRole getOldRole() { return oldRole; }
    public StaffRole getNewRole() { return newRole; }
    public StaffId getChangedBy() { return changedBy; }
}