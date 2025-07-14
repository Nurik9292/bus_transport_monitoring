package tm.ugur.ugur_v3.domain.shared.entities;

import tm.ugur.ugur_v3.domain.shared.valueobjects.EntityId;

public abstract class Entity<ID extends EntityId> {

    private final ID id;

    protected Entity(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("Entity ID cannot be null");
        }
        this.id = id;
    }

    public final ID getId() {
        return id;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Entity<?> other = (Entity<?>) obj;
        return id.equals(other.id);
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s{id=%s}", getClass().getSimpleName(), id);
    }
}