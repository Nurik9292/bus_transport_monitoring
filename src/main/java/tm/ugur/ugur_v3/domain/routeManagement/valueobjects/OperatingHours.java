package tm.ugur.ugur_v3.domain.routeManagement.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Getter
public final class OperatingHours extends ValueObject {

    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Set<DayOfWeek> operatingDays;
    private final boolean operates24Hours;
    private final Map<DayOfWeek, LocalTime> specialStartTimes;
    private final Map<DayOfWeek, LocalTime> specialEndTimes;

    private OperatingHours(
            LocalTime startTime,
            LocalTime endTime,
            Set<DayOfWeek> operatingDays,
            boolean operates24Hours,
            Map<DayOfWeek, LocalTime> specialStartTimes,
            Map<DayOfWeek, LocalTime> specialEndTimes) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.operatingDays = operatingDays != null ? EnumSet.copyOf(operatingDays) : EnumSet.noneOf(DayOfWeek.class);
        this.operates24Hours = operates24Hours;
        this.specialStartTimes = specialStartTimes != null ? new HashMap<>(specialStartTimes) : new HashMap<>();
        this.specialEndTimes = specialEndTimes != null ? new HashMap<>(specialEndTimes) : new HashMap<>();

        validate();
    }

    public static OperatingHours create(LocalTime startTime, LocalTime endTime, Set<DayOfWeek> operatingDays) {
        return new OperatingHours(startTime, endTime, operatingDays, false, null, null);
    }

    public static OperatingHours create24Hours(Set<DayOfWeek> operatingDays) {
        return new OperatingHours(LocalTime.of(0, 0), LocalTime.of(23, 59), operatingDays, true, null, null);
    }

    public static OperatingHours createWeekdays(LocalTime startTime, LocalTime endTime) {
        Set<DayOfWeek> weekdays = EnumSet.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        );
        return new OperatingHours(startTime, endTime, weekdays, false, null, null);
    }

    public static OperatingHours createAllWeek(LocalTime startTime, LocalTime endTime) {
        return new OperatingHours(startTime, endTime, EnumSet.allOf(DayOfWeek.class), false, null, null);
    }

    public boolean isOperatingAt(tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp timestamp) {
        DayOfWeek dayOfWeek = timestamp.toLocalDateTime(ZoneId.systemDefault()).getDayOfWeek();
        LocalTime time = timestamp.toLocalDateTime(ZoneId.systemDefault()).toLocalTime();

        if (!operatingDays.contains(dayOfWeek)) {
            return false;
        }

        if (operates24Hours) {
            return true;
        }

        LocalTime effectiveStart = specialStartTimes.getOrDefault(dayOfWeek, startTime);
        LocalTime effectiveEnd = specialEndTimes.getOrDefault(dayOfWeek, endTime);

        if (effectiveStart.isBefore(effectiveEnd)) {
            return !time.isBefore(effectiveStart) && !time.isAfter(effectiveEnd);
        } else {
            return !time.isBefore(effectiveStart) || !time.isAfter(effectiveEnd);
        }
    }

    public boolean operatesOnDay(DayOfWeek day) {
        return operatingDays.contains(day);
    }

    public LocalTime getStartTimeForDay(DayOfWeek day) {
        return specialStartTimes.getOrDefault(day, startTime);
    }

    public LocalTime getEndTimeForDay(DayOfWeek day) {
        return specialEndTimes.getOrDefault(day, endTime);
    }

    public boolean isOvernight() {
        return !operates24Hours && startTime.isAfter(endTime);
    }

    public EstimatedDuration getDailyOperatingDuration() {
        if (operates24Hours) {
            return EstimatedDuration.ofHours(24);
        }

        long seconds;
        if (isOvernight()) {
            // Calculate overnight duration
            long toMidnight = 86400 - startTime.toSecondOfDay();
            long fromMidnight = endTime.toSecondOfDay();
            seconds = toMidnight + fromMidnight;
        } else {
            seconds = endTime.toSecondOfDay() - startTime.toSecondOfDay();
        }

        return EstimatedDuration.ofSeconds(seconds);
    }

    @Override
    protected void validate() {
        if (!operates24Hours) {
            if (startTime == null || endTime == null) {
                throw new BusinessRuleViolationException(
                        "OPERATING_HOURS",
                        "Start and end times cannot be null for non-24h operation");
            }
        }

        if (operatingDays.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "OPERATING_HOURS_EMPTY",
                    "Must operate on at least one day");
        }

        for (Map.Entry<DayOfWeek, LocalTime> entry : specialStartTimes.entrySet()) {
            if (!operatingDays.contains(entry.getKey())) {
                throw new BusinessRuleViolationException(
                        "OPERATING_HOURS_SPECIAL",
                        "Special start time for non-operating day: " + entry.getKey());
            }
        }

        for (Map.Entry<DayOfWeek, LocalTime> entry : specialEndTimes.entrySet()) {
            if (!operatingDays.contains(entry.getKey())) {
                throw new BusinessRuleViolationException(
                        "OPERATING_HOURS",
                        "Special end time for non-operating day: " + entry.getKey());
            }
        }
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{startTime, endTime, operatingDays, operates24Hours, specialStartTimes, specialEndTimes};
    }

    @Override
    public String toString() {
        if (operates24Hours) {
            return "24/7 on " + operatingDays;
        }
        return String.format("%s-%s on %s", startTime, endTime, operatingDays);
    }
}