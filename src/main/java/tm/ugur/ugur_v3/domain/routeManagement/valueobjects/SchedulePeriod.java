package tm.ugur.ugur_v3.domain.routeManagement.valueobjects;

import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

@Getter
public class SchedulePeriod {
    private final String periodName;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final int headwayMinutes;
    private final Set<DayOfWeek> operatingDays;
    private final boolean isOvernight;

    private SchedulePeriod(String periodName, LocalTime startTime, LocalTime endTime,
                           int headwayMinutes, Set<DayOfWeek> operatingDays) {
        this.periodName = periodName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.headwayMinutes = headwayMinutes;
        this.operatingDays = EnumSet.copyOf(operatingDays);
        this.isOvernight = startTime.isAfter(endTime);
    }

    public static SchedulePeriod create(String periodName, LocalTime startTime, LocalTime endTime,
                                        int headwayMinutes, Set<DayOfWeek> operatingDays) {
        return new SchedulePeriod(periodName, startTime, endTime, headwayMinutes, operatingDays);
    }

    public boolean isTimeInPeriod(LocalTime time) {
        if (isOvernight) {
            return !time.isBefore(startTime) || !time.isAfter(endTime);
        } else {
            return !time.isBefore(startTime) && !time.isAfter(endTime);
        }
    }

    public boolean overlapsWith(SchedulePeriod other) {
        boolean daysOverlap = this.operatingDays.stream()
                .anyMatch(other.operatingDays::contains);

        if (!daysOverlap) {
            return false;
        }

        return isTimeInPeriod(other.startTime) ||
                isTimeInPeriod(other.endTime) ||
                other.isTimeInPeriod(this.startTime) ||
                other.isTimeInPeriod(this.endTime);
    }
}