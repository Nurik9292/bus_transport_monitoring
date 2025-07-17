package tm.ugur.ugur_v3.domain.routeManagement.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Getter
public final class OperatingHours extends ValueObject {

    private final Map<DayOfWeek, TimeSlot> operatingHours;
    private final boolean operates24Hours;
    private final Set<DayOfWeek> operatingDays;

    private OperatingHours(Map<DayOfWeek, TimeSlot> operatingHours, boolean operates24Hours) {
        this.operatingHours = new EnumMap<>(operatingHours);
        this.operates24Hours = operates24Hours;
        this.operatingDays = EnumSet.copyOf(operatingHours.keySet());
    }

    public static OperatingHours create(Map<DayOfWeek, TimeSlot> hours) {
        validateOperatingHours(hours);
        return new OperatingHours(hours, false);
    }

    public static OperatingHours create24Hours(Set<DayOfWeek> operatingDays) {
        Map<DayOfWeek, TimeSlot> hours = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : operatingDays) {
            hours.put(day, TimeSlot.fullDay());
        }
        return new OperatingHours(hours, true);
    }

    public static OperatingHours standardWeekdays(LocalTime startTime, LocalTime endTime) {
        Map<DayOfWeek, TimeSlot> hours = new EnumMap<>(DayOfWeek.class);
        TimeSlot timeSlot = TimeSlot.of(startTime, endTime);

        for (DayOfWeek day : DayOfWeek.values()) {
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                hours.put(day, timeSlot);
            }
        }
        return new OperatingHours(hours, false);
    }

    public boolean isOperatingAt(Timestamp timestamp) {
        DayOfWeek dayOfWeek = timestamp.getDayOfWeek();
        LocalTime time = timestamp.getTime();

        TimeSlot timeSlot = operatingHours.get(dayOfWeek);
        return timeSlot != null && timeSlot.contains(time);
    }

    public boolean isOperatingOn(DayOfWeek dayOfWeek) {
        return operatingDays.contains(dayOfWeek);
    }

    public TimeSlot getHoursFor(DayOfWeek dayOfWeek) {
        return operatingHours.get(dayOfWeek);
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{operatingHours, operates24Hours};
    }

    private static void validateOperatingHours(Map<DayOfWeek, TimeSlot> hours) {
        if (hours == null || hours.isEmpty()) {
            throw new BusinessRuleViolationException("EMPTY_OPERATING_HOURS",
                    "Operating hours cannot be empty");
        }

        for (Map.Entry<DayOfWeek, TimeSlot> entry : hours.entrySet()) {
            if (entry.getValue() == null) {
                throw new BusinessRuleViolationException("NULL_TIME_SLOT",
                        "Time slot cannot be null for " + entry.getKey());
            }
        }
    }

    @Getter
    public static final class TimeSlot {
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final boolean is24Hours;

        private TimeSlot(LocalTime startTime, LocalTime endTime, boolean is24Hours) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.is24Hours = is24Hours;
        }

        public static TimeSlot of(LocalTime startTime, LocalTime endTime) {
            validateTimeSlot(startTime, endTime);
            return new TimeSlot(startTime, endTime, false);
        }

        public static TimeSlot fullDay() {
            return new TimeSlot(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT, true);
        }

        public boolean contains(LocalTime time) {
            if (is24Hours) return true;

            if (endTime.isAfter(startTime)) {
                return !time.isBefore(startTime) && !time.isAfter(endTime);
            } else {
                return !time.isBefore(startTime) || !time.isAfter(endTime);
            }
        }

        public Duration getDuration() {
            if (is24Hours) return Duration.ofHours(24);

            if (endTime.isAfter(startTime)) {
                return Duration.between(startTime, endTime);
            } else {
                return Duration.between(startTime, LocalTime.MIDNIGHT)
                        .plus(Duration.between(LocalTime.MIDNIGHT, endTime));
            }
        }

        private static void validateTimeSlot(LocalTime startTime, LocalTime endTime) {
            if (startTime == null || endTime == null) {
                throw new BusinessRuleViolationException("NULL_TIME",
                        "Start and end times cannot be null");
            }
        }

        @Override
        public String toString() {
            return is24Hours ? "24 hours" : startTime + " - " + endTime;
        }
    }
}