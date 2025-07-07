package tm.ugur.ugur_v3.domain.staffManagement.events;

import tm.ugur.ugur_v3.domain.shared.events.AbstractDomainEvent;
import tm.ugur.ugur_v3.domain.staffManagement.valueobjects.StaffId;

import java.time.LocalDateTime;
import java.util.Objects;

public final class StaffPasswordChangedEvent extends AbstractDomainEvent {

    private final StaffId staffId;
    private final StaffId changedBy;

    public StaffPasswordChangedEvent(StaffId staffId, StaffId changedBy) {
        super();
        this.staffId = Objects.requireNonNull(staffId);
        this.changedBy = Objects.requireNonNull(changedBy);
    }

    @Override
    public String getEventType() {
        return "StaffPasswordChanged";
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
    public StaffId getChangedBy() { return changedBy; }
}
