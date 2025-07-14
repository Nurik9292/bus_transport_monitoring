package tm.ugur.ugur_v3.domain.shared.entities;

import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.EntityId;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AggregateRoot<ID extends EntityId> {

    private final ID id;
    private Long version;
    private final Timestamp createdAt;
    private Timestamp updatedAt;

    private List<DomainEvent> domainEvents;

    protected AggregateRoot(ID id) {
        this.id = id;
        this.version = 0L;
        this.createdAt = Timestamp.now();
        this.updatedAt = this.createdAt;
    }

    protected AggregateRoot(ID id, Long version, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    protected final void addDomainEvent(DomainEvent event) {
        if (domainEvents == null) {
            domainEvents = new ArrayList<>();
        }
        domainEvents.add(event);
    }

    public final List<DomainEvent> getDomainEvents() {
        return domainEvents == null ? Collections.emptyList() : Collections.unmodifiableList(domainEvents);
    }

    public final void clearDomainEvents() {
        if (domainEvents != null) {
            domainEvents.clear();
        }
    }

    public final boolean hasUncommittedEvents() {
        return domainEvents != null && !domainEvents.isEmpty();
    }

    protected final void incrementVersion() {
        this.version++;
        this.updatedAt = Timestamp.now();
    }

    public final void setVersion(Long version) {
        this.version = version;
    }


    protected final void markAsModified() {
        this.updatedAt = Timestamp.now();
        incrementVersion();
    }

    public final ID getId() {
        return id;
    }

    public final Long getVersion() {
        return version;
    }

    public final Timestamp getCreatedAt() {
        return createdAt;
    }

    public final Timestamp getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        AggregateRoot<?> other = (AggregateRoot<?>) obj;
        return id.equals(other.id);
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s{id=%s, version=%d}", getClass().getSimpleName(), id, version);
    }
}