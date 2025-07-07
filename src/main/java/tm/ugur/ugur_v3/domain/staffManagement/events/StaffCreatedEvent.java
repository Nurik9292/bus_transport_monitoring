package tm.ugur.ugur_v3.domain.staffManagement.events;

import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.staffManagement.enums.StaffRole;
import tm.ugur.ugur_v3.domain.staffManagement.valueobjects.Email;
import tm.ugur.ugur_v3.domain.staffManagement.valueobjects.StaffId;
import tm.ugur.ugur_v3.domain.staffManagement.valueobjects.StaffName;

import java.time.LocalDateTime;
import java.util.Objects;

public class StaffCreatedEvent implements DomainEvent {

    private final StaffId staffId;
    private final StaffName name;
    private final Email email;
    private final StaffRole role;
    private final StaffId createdBy;

    public StaffCreatedEvent(StaffId staffId, StaffName name, Email email,
                             StaffRole role, StaffId createdBy) {
        super();
        this.staffId = Objects.requireNonNull(staffId);
        this.name = Objects.requireNonNull(name);
        this.email = Objects.requireNonNull(email);
        this.role = Objects.requireNonNull(role);
        this.createdBy = Objects.requireNonNull(createdBy);
    }

    @Override
    public String getEventType() {
        return "StaffCreated";
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
    public StaffName getName() { return name; }
    public Email getEmail() { return email; }
    public StaffRole getRole() { return role; }
    public StaffId getCreatedBy() { return createdBy; }
}
