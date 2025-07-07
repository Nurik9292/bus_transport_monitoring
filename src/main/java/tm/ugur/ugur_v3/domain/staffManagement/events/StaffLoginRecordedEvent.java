package tm.ugur.ugur_v3.domain.staffManagement.events;

import tm.ugur.ugur_v3.domain.shared.events.AbstractDomainEvent;
import tm.ugur.ugur_v3.domain.staffManagement.valueobjects.StaffId;

import java.time.LocalDateTime;
import java.util.Objects;

public final class StaffLoginRecordedEvent extends AbstractDomainEvent {

    private final StaffId staffId;
    private final java.time.LocalDateTime loginTime;

    public StaffLoginRecordedEvent(StaffId staffId, java.time.LocalDateTime loginTime) {
        super();
        this.staffId = Objects.requireNonNull(staffId);
        this.loginTime = Objects.requireNonNull(loginTime);
    }

    @Override
    public String getEventType() {
        return "StaffLoginRecorded";
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
    public java.time.LocalDateTime getLoginTime() { return loginTime; }
}
