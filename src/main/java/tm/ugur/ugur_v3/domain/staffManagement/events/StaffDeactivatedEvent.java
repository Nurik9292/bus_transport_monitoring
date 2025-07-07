package tm.ugur.ugur_v3.domain.staffManagement.events;

import tm.ugur.ugur_v3.domain.shared.events.AbstractDomainEvent;
import tm.ugur.ugur_v3.domain.staffManagement.valueobjects.StaffId;

import java.time.LocalDateTime;
import java.util.Objects;

public final class StaffDeactivatedEvent extends AbstractDomainEvent {

    private final StaffId staffId;
    private final StaffId deactivatedBy;
    private final String reason;

    public StaffDeactivatedEvent(StaffId staffId, StaffId deactivatedBy, String reason) {
        super();
        this.staffId = Objects.requireNonNull(staffId);
        this.deactivatedBy = Objects.requireNonNull(deactivatedBy);
        this.reason = reason;
    }

    @Override
    public String getEventType() {
        return "StaffDeactivated";
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
    public StaffId getDeactivatedBy() { return deactivatedBy; }
    public String getReason() { return reason; }
}
