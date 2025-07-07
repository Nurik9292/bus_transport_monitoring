package tm.ugur.ugur_v3.domain.shared.events;

import java.time.LocalDateTime;

public interface DomainEvent {

    String getEventType();
    String getAggregateId();
    LocalDateTime getOccurredAt();

    default String getEventVersion() {
        return "1.0";
    }
}
