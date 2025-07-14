package tm.ugur.ugur_v3.domain.shared.valueobjects;

public final class AuditInfo extends ValueObject {

    private final String createdBy;
    private final Timestamp createdAt;
    private final String lastModifiedBy;
    private final Timestamp lastModifiedAt;

    private AuditInfo(String createdBy, Timestamp createdAt, String lastModifiedBy, Timestamp lastModifiedAt) {
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.lastModifiedBy = lastModifiedBy;
        this.lastModifiedAt = lastModifiedAt;
        validate();
    }

    public static AuditInfo create(String createdBy) {
        Timestamp now = Timestamp.now();
        return new AuditInfo(createdBy, now, createdBy, now);
    }

    public AuditInfo updateModifiedBy(String modifiedBy) {
        return new AuditInfo(this.createdBy, this.createdAt, modifiedBy, Timestamp.now());
    }

    @Override
    protected void validate() {
        if (createdBy == null || createdBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Created by cannot be null or empty");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Created at cannot be null");
        }
        if (lastModifiedBy == null || lastModifiedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Last modified by cannot be null or empty");
        }
        if (lastModifiedAt == null) {
            throw new IllegalArgumentException("Last modified at cannot be null");
        }
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Timestamp getLastModifiedAt() {
        return lastModifiedAt;
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{createdBy, createdAt, lastModifiedBy, lastModifiedAt};
    }
}