package tm.ugur.ugur_v3.domain.shared.exceptions;

public final class ConcurrencyException extends DomainException {

    private final String aggregateId;
    private final Long expectedVersion;
    private final Long actualVersion;

    public ConcurrencyException(String aggregateId, Long expectedVersion, Long actualVersion) {
        super(String.format("Concurrency conflict for aggregate %s: expected version %d, actual version %d",
                aggregateId, expectedVersion, actualVersion), "CONCURRENCY_CONFLICT");
        this.aggregateId = aggregateId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    public String getAggregateId() { return aggregateId; }
    public Long getExpectedVersion() { return expectedVersion; }
    public Long getActualVersion() { return actualVersion; }
}