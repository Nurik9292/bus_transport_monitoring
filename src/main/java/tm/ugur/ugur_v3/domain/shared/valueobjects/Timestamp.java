package tm.ugur.ugur_v3.domain.shared.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.InvalidTimestampException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Getter
public final class Timestamp extends ValueObject {

    private static final long MIN_TIMESTAMP = 946684800000L; // 2000-01-01 UTC
    private static final long MAX_TIMESTAMP = 4102444800000L; // 2100-01-01 UTC
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final long epochMillis;

    private Timestamp(long epochMillis) {
        this.epochMillis = epochMillis;
        validate();
    }

    public static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    public static Timestamp of(long epochMillis) {
        return new Timestamp(epochMillis);
    }

    public static Timestamp of(Instant instant) {
        return new Timestamp(instant.toEpochMilli());
    }

    public static Timestamp of(LocalDateTime localDateTime) {
        return new Timestamp(localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli());
    }

    @Override
    protected void validate() {
        if (epochMillis < MIN_TIMESTAMP || epochMillis > MAX_TIMESTAMP) {
            throw new InvalidTimestampException(
                    String.format("Timestamp must be between %d and %d, got: %d",
                            MIN_TIMESTAMP, MAX_TIMESTAMP, epochMillis));
        }
    }

    public boolean isBefore(Timestamp other) {
        return this.epochMillis < other.epochMillis;
    }

    public boolean isAfter(Timestamp other) {
        return this.epochMillis > other.epochMillis;
    }

    public boolean isPast() {
        return epochMillis < System.currentTimeMillis();
    }

    public boolean isFuture() {
        return epochMillis > System.currentTimeMillis();
    }

    public long differenceMillis(Timestamp other) {
        return Math.abs(this.epochMillis - other.epochMillis);
    }

    public Timestamp plusMillis(long millis) {
        return new Timestamp(this.epochMillis + millis);
    }

    public Timestamp minusMillis(long millis) {
        return new Timestamp(this.epochMillis - millis);
    }


    public LocalDateTime toLocalDateTime(ZoneId zoneId) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zoneId);
    }


    public LocalDateTime toUtcLocalDateTime() {
        return toLocalDateTime(ZoneOffset.UTC);
    }

    public Instant toInstant() {
        return Instant.ofEpochMilli(epochMillis);
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{epochMillis};
    }

    @Override
    public String toString() {
        return ISO_FORMATTER.format(toInstant());
    }
}