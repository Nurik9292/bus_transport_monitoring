package tm.ugur.ugur_v3.domain.shared.events;

import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

public interface DomainEvent {

    String getEventType();

    String getEventId();

    String getAggregateId();

    Timestamp getOccurredAt();

    String getAggregateType();

    default Long getVersion() {
        return 1L;
    }

    default String getCorrelationId() {
        return null;
    }

    default java.util.Map<String, Object> getMetadata() {
        return java.util.Collections.emptyMap();
    }
}
