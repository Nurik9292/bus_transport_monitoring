package tm.ugur.ugur_v3.domain.shared.events;

import java.time.LocalDateTime;

public abstract class AbstractDomainEvent implements DomainEvent {

    private final LocalDateTime occurredAt;
    private final String eventId;

    protected AbstractDomainEvent() {
        this.occurredAt = LocalDateTime.now();
        this.eventId = java.util.UUID.randomUUID().toString();
    }

    public String getEventId() {
        return eventId;
    }
}
