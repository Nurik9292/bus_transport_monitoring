package tm.ugur.ugur_v3.domain.shared.events;

import java.util.List;

public interface EventStore {

    void saveEvents(String aggregateId, List<DomainEvent> events, Long expectedVersion);

    List<DomainEvent> getEventsForAggregate(String aggregateId);

    List<DomainEvent> getEventsForAggregateFromVersion(String aggregateId, Long fromVersion);

    List<DomainEvent> getEventsForAggregateInRange(String aggregateId, Long fromVersion, Long toVersion);

    Long getCurrentVersion(String aggregateId);

    boolean aggregateExists(String aggregateId);

    void saveSnapshot(String aggregateId, Object snapshot, Long version);

    <T> T getSnapshot(String aggregateId, Class<T> snapshotType);
}