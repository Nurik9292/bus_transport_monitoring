package tm.ugur.ugur_v3.domain.routeManagement.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Getter
public final class ScheduleAdjustment extends ValueObject {

    private static final int MAX_ADJUSTMENT_MINUTES = 60;
    private static final int MIN_ADJUSTMENT_MINUTES = -30;
    private static final int MAX_REASON_LENGTH = 200;
    private static final int MAX_ADJUSTED_BY_LENGTH = 100;

    private final String id;
    private final DayOfWeek dayOfWeek;
    private final LocalTime originalTime;
    private final int adjustmentMinutes;
    private final String reason;
    private final String adjustedBy;
    private final Timestamp createdAt;
    private final AdjustmentType adjustmentType;
    private final AdjustmentSeverity severity;

    private ScheduleAdjustment(String id, DayOfWeek dayOfWeek, LocalTime originalTime,
                               int adjustmentMinutes, String reason, String adjustedBy,
                               Timestamp createdAt) {
        this.id = validateId(id);
        this.dayOfWeek = validateDayOfWeek(dayOfWeek);
        this.originalTime = validateOriginalTime(originalTime);
        this.adjustmentMinutes = validateAdjustmentMinutes(adjustmentMinutes);
        this.reason = validateReason(reason);
        this.adjustedBy = validateAdjustedBy(adjustedBy);
        this.createdAt = validateCreatedAt(createdAt);
        this.adjustmentType = determineAdjustmentType(adjustmentMinutes);
        this.severity = determineSeverity(adjustmentMinutes);
    }


    public static ScheduleAdjustment create(
            String id,
            DayOfWeek dayOfWeek,
            LocalTime originalTime,
            int adjustmentMinutes,
            String reason,
            String adjustedBy,
            Timestamp time
    ) {
        return new ScheduleAdjustment(
               id,
                dayOfWeek,
                originalTime,
                adjustmentMinutes,
                reason,
                adjustedBy,
                time
        );
    }

    public static ScheduleAdjustment delay(DayOfWeek dayOfWeek, LocalTime originalTime,
                                           int delayMinutes, String reason, String adjustedBy) {
        if (delayMinutes <= 0) {
            throw new BusinessRuleViolationException(
                    "INVALID_DELAY_MINUTES",
                    "Delay minutes must be positive: " + delayMinutes
            );
        }
        return create(
                generateId(dayOfWeek, originalTime),
                dayOfWeek,
                originalTime,
                delayMinutes,
                reason,
                adjustedBy,
                Timestamp.now()
        );
    }

    public static ScheduleAdjustment advance(DayOfWeek dayOfWeek, LocalTime originalTime,
                                             int advanceMinutes, String reason, String adjustedBy) {
        if (advanceMinutes <= 0) {
            throw new BusinessRuleViolationException(
                    "INVALID_ADVANCE_MINUTES",
                    "Advance minutes must be positive: " + advanceMinutes
            );
        }
        return create(
                generateId(dayOfWeek, originalTime),
                dayOfWeek,
                originalTime,
                -advanceMinutes,
                reason,
                adjustedBy,
                Timestamp.now()
                );
    }

    public static ScheduleAdjustment fromExisting(String existingId, DayOfWeek dayOfWeek,
                                                  LocalTime originalTime, int adjustmentMinutes,
                                                  String reason, String adjustedBy, Timestamp createdAt) {
        return new ScheduleAdjustment(existingId, dayOfWeek, originalTime, adjustmentMinutes,
                reason, adjustedBy, createdAt);
    }


    public boolean isDelay() {
        return adjustmentMinutes > 0;
    }

    public boolean isEarlyDeparture() {
        return adjustmentMinutes < 0;
    }

    public boolean isSignificant() {
        return Math.abs(adjustmentMinutes) > 10;
    }

    public boolean requiresPassengerNotification() {
        return severity.ordinal() >= AdjustmentSeverity.MEDIUM.ordinal();
    }

    public boolean isCritical() {
        return severity == AdjustmentSeverity.CRITICAL;
    }


    public LocalTime getAdjustedTime() {
        return originalTime.plusMinutes(adjustmentMinutes);
    }

    public int getAbsoluteAdjustmentMinutes() {
        return Math.abs(adjustmentMinutes);
    }

    public boolean isCompatibleWith(ScheduleAdjustment other) {
        return this.dayOfWeek.equals(other.dayOfWeek) &&
                this.originalTime.equals(other.originalTime);
    }

    public boolean isExpired() {
        return createdAt.isOlderThan(Timestamp.now().minusHours(24));
    }


    public String getDescription() {
        String direction = isDelay() ? "delayed" : "advanced";
        return String.format("%s %s %s by %d min (%s) - %s",
                dayOfWeek, originalTime, direction, getAbsoluteAdjustmentMinutes(),
                severity, reason);
    }

    // ================== VALIDATION METHODS ==================

    private static String validateId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new BusinessRuleViolationException(
                    "INVALID_ADJUSTMENT_ID",
                    "Adjustment ID cannot be null or empty"
            );
        }
        return id.trim();
    }

    private static DayOfWeek validateDayOfWeek(DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) {
            throw new BusinessRuleViolationException(
                    "INVALID_DAY_OF_WEEK",
                    "Day of week cannot be null"
            );
        }
        return dayOfWeek;
    }

    private static LocalTime validateOriginalTime(LocalTime originalTime) {
        if (originalTime == null) {
            throw new BusinessRuleViolationException(
                    "INVALID_ORIGINAL_TIME",
                    "Original time cannot be null"
            );
        }
        return originalTime;
    }

    private static int validateAdjustmentMinutes(int adjustmentMinutes) {
        if (adjustmentMinutes < MIN_ADJUSTMENT_MINUTES || adjustmentMinutes > MAX_ADJUSTMENT_MINUTES) {
            throw new BusinessRuleViolationException(
                    "INVALID_ADJUSTMENT_RANGE",
                    String.format("Adjustment must be between %d and %d minutes: %d",
                            MIN_ADJUSTMENT_MINUTES, MAX_ADJUSTMENT_MINUTES, adjustmentMinutes)
            );
        }
        if (adjustmentMinutes == 0) {
            throw new BusinessRuleViolationException(
                    "ZERO_ADJUSTMENT",
                    "Adjustment cannot be zero minutes"
            );
        }
        return adjustmentMinutes;
    }

    private static String validateReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessRuleViolationException(
                    "INVALID_REASON",
                    "Adjustment reason cannot be null or empty"
            );
        }
        if (reason.length() > MAX_REASON_LENGTH) {
            throw new BusinessRuleViolationException(
                    "REASON_TOO_LONG",
                    String.format("Reason cannot exceed %d characters: %d",
                            MAX_REASON_LENGTH, reason.length())
            );
        }
        return reason.trim();
    }

    private static String validateAdjustedBy(String adjustedBy) {
        if (adjustedBy == null || adjustedBy.trim().isEmpty()) {
            throw new BusinessRuleViolationException(
                    "INVALID_ADJUSTED_BY",
                    "Adjusted by cannot be null or empty"
            );
        }
        if (adjustedBy.length() > MAX_ADJUSTED_BY_LENGTH) {
            throw new BusinessRuleViolationException(
                    "ADJUSTED_BY_TOO_LONG",
                    String.format("Adjusted by cannot exceed %d characters: %d",
                            MAX_ADJUSTED_BY_LENGTH, adjustedBy.length())
            );
        }
        return adjustedBy.trim();
    }

    private static Timestamp validateCreatedAt(Timestamp createdAt) {
        if (createdAt == null) {
            throw new BusinessRuleViolationException(
                    "INVALID_CREATED_AT",
                    "Created at timestamp cannot be null"
            );
        }
        if (createdAt.isAfter(Timestamp.now().plusMinutes(5))) {
            throw new BusinessRuleViolationException(
                    "FUTURE_TIMESTAMP",
                    "Created at cannot be in the future"
            );
        }
        return createdAt;
    }


    private static String generateId(DayOfWeek dayOfWeek, LocalTime originalTime) {
        return String.format("%s_%s_%s", dayOfWeek, originalTime, UUID.randomUUID().toString().substring(0, 8));
    }

    private static AdjustmentType determineAdjustmentType(int adjustmentMinutes) {
        return adjustmentMinutes > 0 ? AdjustmentType.DELAY : AdjustmentType.ADVANCE;
    }

    private static AdjustmentSeverity determineSeverity(int adjustmentMinutes) {
        int absMinutes = Math.abs(adjustmentMinutes);
        if (absMinutes >= 30) return AdjustmentSeverity.CRITICAL;
        if (absMinutes >= 15) return AdjustmentSeverity.HIGH;
        if (absMinutes >= 5) return AdjustmentSeverity.MEDIUM;
        return AdjustmentSeverity.LOW;
    }


    @Getter
    public enum AdjustmentType {
        DELAY("Задержка", "Отправление перенесено на более позднее время"),
        ADVANCE("Опережение", "Отправление перенесено на более раннее время");

        private final String displayName;
        private final String description;

        AdjustmentType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        @Override
        public String toString() { return displayName; }
    }

    @Getter
    public enum AdjustmentSeverity {
        LOW("Незначительная", "green", "Корректировка менее 5 минут"),
        MEDIUM("Средняя", "yellow", "Корректировка 5-14 минут"),
        HIGH("Высокая", "orange", "Корректировка 15-29 минут"),
        CRITICAL("Критическая", "red", "Корректировка 30+ минут");

        private final String displayName;
        private final String colorCode;
        private final String description;

        AdjustmentSeverity(String displayName, String colorCode, String description) {
            this.displayName = displayName;
            this.colorCode = colorCode;
            this.description = description;
        }

        @Override
        public String toString() { return displayName; }
    }


    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{id, dayOfWeek, originalTime, adjustmentMinutes, reason, adjustedBy, createdAt};
    }

    @Override
    public String toString() {
        String direction = isDelay() ? "delay" : "early";
        return String.format("ScheduleAdjustment{%s %s: %d min %s (%s) by '%s'}",
                dayOfWeek, originalTime, getAbsoluteAdjustmentMinutes(), direction, reason, adjustedBy);
    }

    public String toDisplayString() {
        String direction = isDelay() ? "задержка" : "опережение";
        return String.format("%s %s: %s на %d мин",
                dayOfWeek.name(), originalTime, direction, getAbsoluteAdjustmentMinutes());
    }

    public String getAdjustmentKey() {
        return dayOfWeek + "_" + originalTime.toString();
    }
}