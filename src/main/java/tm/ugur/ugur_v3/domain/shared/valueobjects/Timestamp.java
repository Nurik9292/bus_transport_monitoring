package tm.ugur.ugur_v3.domain.shared.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.InvalidTimestampException;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

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

    // ====================== FACTORY METHODS ======================

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

    public static Timestamp parse(String isoString) {
        try {
            Instant instant = Instant.parse(isoString);
            return Timestamp.of(instant);
        } catch (Exception e) {
            throw new InvalidTimestampException("Invalid timestamp format: " + isoString);
        }
    }

    public static Timestamp startOfToday() {
        return Timestamp.now().startOfDay();
    }

    public static Timestamp endOfToday() {
        return Timestamp.now().endOfDay();
    }

    // ====================== COMPARISON METHODS ======================

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

    // ====================== NEW: ENHANCED COMPARISON METHODS ======================

    public boolean isOlderThan(Timestamp other) {
        return this.isBefore(other);
    }

    public boolean isNewerThan(Timestamp other) {
        return this.isAfter(other);
    }

    public boolean isOlderThanHours(long hours) {
        Timestamp compareTime = Timestamp.now().minusHours(hours);
        return this.isBefore(compareTime);
    }

    public boolean isOlderThanDays(long days) {
        Timestamp compareTime = Timestamp.now().minusDays(days);
        return this.isBefore(compareTime);
    }

    public boolean isOlderThanMinutes(long minutes) {
        Timestamp compareTime = Timestamp.now().minusMinutes(minutes);
        return this.isBefore(compareTime);
    }

    // ====================== ARITHMETIC OPERATIONS ======================

    public Timestamp plusMillis(long millis) {
        return new Timestamp(this.epochMillis + millis);
    }

    public Timestamp minusMillis(long millis) {
        return new Timestamp(this.epochMillis - millis);
    }

    public Timestamp plusSeconds(long seconds) {
        return new Timestamp(this.epochMillis + (seconds * 1000));
    }

    public Timestamp minusSeconds(long seconds) {
        return new Timestamp(this.epochMillis - (seconds * 1000));
    }

    public Timestamp plusMinutes(long minutes) {
        return new Timestamp(this.epochMillis + (minutes * 60 * 1000));
    }

    public Timestamp minusMinutes(long minutes) {
        return new Timestamp(this.epochMillis - (minutes * 60 * 1000));
    }

    public Timestamp plusHours(long hours) {
        return new Timestamp(this.epochMillis + (hours * 60 * 60 * 1000));
    }

    public Timestamp minusHours(long hours) {
        return new Timestamp(this.epochMillis - (hours * 60 * 60 * 1000));
    }

    public Timestamp plusDays(long days) {
        return new Timestamp(this.epochMillis + (days * 24 * 60 * 60 * 1000));
    }

    public Timestamp minusDays(long days) {
        return new Timestamp(this.epochMillis - (days * 24 * 60 * 60 * 1000));
    }

    public Timestamp plusWeeks(long weeks) {
        return plusDays(weeks * 7);
    }

    public Timestamp minusWeeks(long weeks) {
        return minusDays(weeks * 7);
    }

    // ====================== CONVERSION METHODS ======================

    public LocalDateTime toLocalDateTime(ZoneId zoneId) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zoneId);
    }

    public LocalDateTime toLocalDateTime() {
        return toLocalDateTime(ZoneId.systemDefault());
    }

    public LocalDateTime toUtcLocalDateTime() {
        return toLocalDateTime(ZoneOffset.UTC);
    }

    public Instant toInstant() {
        return Instant.ofEpochMilli(epochMillis);
    }

    // ====================== NEW: TIME COMPONENT EXTRACTION ======================

    public DayOfWeek getDayOfWeek() {
        return toLocalDateTime().getDayOfWeek();
    }

    public LocalTime getTime() {
        return toLocalDateTime().toLocalTime();
    }

    public LocalDate getDate() {
        return toLocalDateTime().toLocalDate();
    }

    public int getHour() {
        return toLocalDateTime().getHour();
    }

    public int getMinute() {
        return toLocalDateTime().getMinute();
    }

    public int getSecond() {
        return toLocalDateTime().getSecond();
    }

    // ====================== NEW: TIME RANGE AND DAY CHECKS ======================

    public boolean isTimeInRange(LocalTime startTime, LocalTime endTime) {
        LocalTime currentTime = getTime();

        if (endTime.isAfter(startTime)) {
            // Обычный диапазон в течение дня (например, 08:00 - 18:00)
            return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
        } else {
            // Диапазон через полночь (например, 22:00 - 06:00)
            return !currentTime.isBefore(startTime) || !currentTime.isAfter(endTime);
        }
    }

    public boolean isWeekday() {
        DayOfWeek day = getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    public boolean isWeekend() {
        DayOfWeek day = getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    // ====================== DURATION CALCULATIONS ======================

    public long hoursBetween(Timestamp other) {
        return Math.abs(this.epochMillis - other.epochMillis) / (60 * 60 * 1000);
    }

    public long daysBetween(Timestamp other) {
        return Math.abs(this.epochMillis - other.epochMillis) / (24 * 60 * 60 * 1000);
    }

    public boolean isSameDay(Timestamp other) {
        LocalDateTime thisDay = toUtcLocalDateTime().truncatedTo(ChronoUnit.DAYS);
        LocalDateTime otherDay = other.toUtcLocalDateTime().truncatedTo(ChronoUnit.DAYS);
        return thisDay.equals(otherDay);
    }

    public Timestamp startOfDay() {
        LocalDateTime startOfDay = toUtcLocalDateTime().truncatedTo(ChronoUnit.DAYS);
        return Timestamp.of(startOfDay);
    }

    public Timestamp endOfDay() {
        LocalDateTime endOfDay = toUtcLocalDateTime().truncatedTo(ChronoUnit.DAYS)
                .plusDays(1).minusNanos(1);
        return Timestamp.of(endOfDay);
    }

    public boolean isBetween(Timestamp start, Timestamp end) {
        return !this.isBefore(start) && !this.isAfter(end);
    }

    // ====================== FORMATTING METHODS ======================

    public String toLogFormat() {
        return toUtcLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String toUserFormat() {
        return toLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    // ====================== VALIDATION ======================

    @Override
    protected void validate() {
        if (epochMillis < MIN_TIMESTAMP || epochMillis > MAX_TIMESTAMP) {
            throw new InvalidTimestampException(
                    String.format("Timestamp must be between %d and %d, got: %d",
                            MIN_TIMESTAMP, MAX_TIMESTAMP, epochMillis));
        }
    }

    // ====================== VALUE OBJECT IMPLEMENTATION ======================

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{epochMillis};
    }

    @Override
    public String toString() {
        return ISO_FORMATTER.format(toInstant());
    }

    // ====================== UTILITY METHODS FOR SPECIFIC USE CASES ======================

    public Timestamp withWorkingHoursStart() {
        return withTime(LocalTime.of(9, 0));
    }

    public Timestamp withWorkingHoursEnd() {
        return withTime(LocalTime.of(18, 0));
    }

    public Timestamp withTime(LocalTime time) {
        LocalDate currentDate = getDate();
        LocalDateTime newDateTime = LocalDateTime.of(currentDate, time);
        return Timestamp.of(newDateTime);
    }

    public boolean isInWorkingHours() {
        return isWeekday() && isTimeInRange(LocalTime.of(9, 0), LocalTime.of(18, 0));
    }

    public Timestamp nextWorkingDay() {
        Timestamp next = this.plusDays(1);
        while (next.isWeekend()) {
            next = next.plusDays(1);
        }
        return next.withTime(LocalTime.of(9, 0));
    }
}