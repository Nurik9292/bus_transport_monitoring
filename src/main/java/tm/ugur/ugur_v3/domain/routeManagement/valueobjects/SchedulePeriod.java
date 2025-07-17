package tm.ugur.ugur_v3.domain.routeManagement.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

@Getter
public final class SchedulePeriod extends ValueObject {

    private final String name;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Set<DayOfWeek> operatingDays;
    private final int headwayMinutes;
    private final PeriodType periodType;

    private SchedulePeriod(String name, LocalTime startTime, LocalTime endTime,
                           Set<DayOfWeek> operatingDays, int headwayMinutes, PeriodType periodType) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.operatingDays = EnumSet.copyOf(operatingDays);
        this.headwayMinutes = headwayMinutes;
        this.periodType = periodType;
    }

    public static SchedulePeriod create(String name, LocalTime startTime, LocalTime endTime,
                                        Set<DayOfWeek> operatingDays, int headwayMinutes, PeriodType periodType) {
        validatePeriod(name, startTime, endTime, operatingDays, headwayMinutes);
        return new SchedulePeriod(name, startTime, endTime, operatingDays, headwayMinutes, periodType);
    }

    public static SchedulePeriod rushHour(String name, LocalTime startTime, LocalTime endTime,
                                          Set<DayOfWeek> operatingDays, int headwayMinutes) {
        return create(name, startTime, endTime, operatingDays, headwayMinutes, PeriodType.RUSH_HOUR);
    }

    public static SchedulePeriod regular(String name, LocalTime startTime, LocalTime endTime,
                                         Set<DayOfWeek> operatingDays, int headwayMinutes) {
        return create(name, startTime, endTime, operatingDays, headwayMinutes, PeriodType.REGULAR);
    }

    public static SchedulePeriod night(String name, LocalTime startTime, LocalTime endTime,
                                       Set<DayOfWeek> operatingDays, int headwayMinutes) {
        return create(name, startTime, endTime, operatingDays, headwayMinutes, PeriodType.NIGHT);
    }

    public boolean isTimeInPeriod(LocalTime time) {
        if (endTime.isAfter(startTime)) {
            return !time.isBefore(startTime) && !time.isAfter(endTime);
        } else {
            return !time.isBefore(startTime) || !time.isAfter(endTime);
        }
    }

    public boolean overlapsWith(SchedulePeriod other) {
        Set<DayOfWeek> commonDays = EnumSet.copyOf(this.operatingDays);
        commonDays.retainAll(other.operatingDays);

        if (commonDays.isEmpty()) {
            return false; // No common days
        }

        return timePeriodsOverlap(this.startTime, this.endTime, other.startTime, other.endTime);
    }

    private boolean timePeriodsOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        boolean period1Overnight = end1.isBefore(start1);
        boolean period2Overnight = end2.isBefore(start2);

        if (!period1Overnight && !period2Overnight) {
            return start1.isBefore(end2) && start2.isBefore(end1);
        }

        if (period1Overnight && period2Overnight) {
            return true; // Both overnight periods will overlap
        }

        if (period1Overnight) {
            return start2.isBefore(end1) || start1.isBefore(end2);
        } else {
            return start1.isBefore(end2) || start2.isBefore(end1);
        }
    }

    public java.time.Duration getDuration() {
        if (endTime.isAfter(startTime)) {
            return java.time.Duration.between(startTime, endTime);
        } else {
            return java.time.Duration.between(startTime, LocalTime.MIDNIGHT)
                    .plus(java.time.Duration.between(LocalTime.MIDNIGHT, endTime));
        }
    }

    public boolean isRushHour() {
        return periodType == PeriodType.RUSH_HOUR;
    }

    public boolean isNightPeriod() {
        return periodType == PeriodType.NIGHT;
    }

    private static void validatePeriod(String name, LocalTime startTime, LocalTime endTime,
                                       Set<DayOfWeek> operatingDays, int headwayMinutes) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessRuleViolationException("INVALID_PERIOD_NAME", "Period name cannot be empty");
        }

        if (startTime == null || endTime == null) {
            throw new BusinessRuleViolationException("NULL_TIME", "Start and end times cannot be null");
        }

        if (operatingDays == null || operatingDays.isEmpty()) {
            throw new BusinessRuleViolationException("NO_OPERATING_DAYS", "Must specify at least one operating day");
        }

        if (headwayMinutes < 1 || headwayMinutes > 240) {
            throw new BusinessRuleViolationException("INVALID_HEADWAY",
                    "Headway must be between 1 and 240 minutes");
        }
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{name, startTime, endTime, operatingDays, headwayMinutes, periodType};
    }

    @Override
    public String toString() {
        return String.format("%s: %s-%s (%d min intervals) [%s]",
                name, startTime, endTime, headwayMinutes, periodType);
    }

    public enum PeriodType {
        RUSH_HOUR("Час пик"),
        REGULAR("Обычное время"),
        NIGHT("Ночное время"),
        WEEKEND("Выходные");

        private final String displayName;

        PeriodType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}