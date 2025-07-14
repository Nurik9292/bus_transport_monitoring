package tm.ugur.ugur_v3.domain.shared.valueobjects;

import tm.ugur.ugur_v3.domain.shared.exceptions.InvalidEntityIdException;

public abstract class EntityId extends ValueObject {

    private final String value;

    protected EntityId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidEntityIdException("Entity ID cannot be null or empty");
        }
        this.value = value.trim().intern();
        validate();
    }

    @Override
    protected void validate() {
        if (value.length() > 100) {
            throw new InvalidEntityIdException("Entity ID too long: " + value.length());
        }
    }

    public String getValue() {
        return value;
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{value};
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + value + "}";
    }
}