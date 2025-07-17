package tm.ugur.ugur_v3.domain.routeManagement.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public final class DailySchedule extends ValueObject {

    private static final int MAX_DEPARTURES_PER_DAY = 500;
    private static final int MIN_INTERVAL_MINUTES = 1;

    private final List<LocalTime> departures;
    private final int tripCount;
    private final LocalTime firstDeparture;
    private final LocalTime lastDeparture;
    private final boolean isActive;

    private DailySchedule(List<LocalTime> departures, boolean isActive) {
        this.departures = List.copyOf(departures);
        this.tripCount = departures.size();
        this.firstDeparture = departures.isEmpty() ? null : departures.getFirst();
        this.lastDeparture = departures.isEmpty() ? null : departures.getLast();
        this.isActive = isActive;
    }

    public static DailySchedule create(List<LocalTime> departures) {
        validateDepartures(departures);
        List<LocalTime> sortedDepartures = departures.stream()
                .sorted()
                .collect(Collectors.toList());
        return new DailySchedule(sortedDepartures, true);
    }

    public static DailySchedule createWithInterval(LocalTime startTime, LocalTime endTime, int intervalMinutes) {
        validateInterval(startTime, endTime, intervalMinutes);

        List<LocalTime> departures = new ArrayList<>();
        LocalTime current = startTime;

        while (!current.isAfter(endTime)) {
            departures.add(current);
            current = current.plusMinutes(intervalMinutes);
        }

        return new DailySchedule(departures, true);
    }

    public static DailySchedule empty() {
        return new DailySchedule(Collections.emptyList(), false);
    }

    public Optional<LocalTime> getNextDeparture(LocalTime fromTime) {
        return departures.stream()
                .filter(time -> time.isAfter(fromTime))
                .findFirst();
    }

    public List<LocalTime> getDeparturesAfter(LocalTime time) {
        return departures.stream()
                .filter(departure -> departure.isAfter(time))
                .collect(Collectors.toList());
    }

    public List<LocalTime> getDeparturesBetween(LocalTime startTime, LocalTime endTime) {
        return departures.stream()
                .filter(time -> !time.isBefore(startTime) && !time.isAfter(endTime))
                .collect(Collectors.toList());
    }

    public boolean hasDepartureAt(LocalTime time) {
        return departures.contains(time);
    }

    public int getAverageIntervalMinutes() {
        if (departures.size() < 2) return 0;

        int totalMinutes = 0;
        for (int i = 1; i < departures.size(); i++) {
            LocalTime prev = departures.get(i - 1);
            LocalTime curr = departures.get(i);
            totalMinutes += (int) java.time.Duration.between(prev, curr).toMinutes();
        }

        return totalMinutes / (departures.size() - 1);
    }

    public List<LocalTime> getAllDepartures() {
        return departures;
    }

    private static void validateDepartures(List<LocalTime> departures) {
        if (departures == null) {
            throw new BusinessRuleViolationException("NULL_DEPARTURES", "Departures list cannot be null");
        }

        if (departures.size() > MAX_DEPARTURES_PER_DAY) {
            throw new BusinessRuleViolationException("TOO_MANY_DEPARTURES",
                    "Cannot have more than " + MAX_DEPARTURES_PER_DAY + " departures per day");
        }

        Set<LocalTime> uniqueTimes = new HashSet<>(departures);
        if (uniqueTimes.size() != departures.size()) {
            throw new BusinessRuleViolationException("DUPLICATE_DEPARTURES",
                    "Departure times must be unique");
        }
    }

    private static void validateInterval(LocalTime startTime, LocalTime endTime, int intervalMinutes) {
        if (startTime == null || endTime == null) {
            throw new BusinessRuleViolationException("NULL_TIME", "Start and end times cannot be null");
        }

        if (startTime.isAfter(endTime)) {
            throw new BusinessRuleViolationException("INVALID_TIME_RANGE",
                    "Start time cannot be after end time");
        }

        if (intervalMinutes < MIN_INTERVAL_MINUTES) {
            throw new BusinessRuleViolationException("INTERVAL_TOO_SHORT",
                    "Interval must be at least " + MIN_INTERVAL_MINUTES + " minute(s)");
        }
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{departures, isActive};
    }

    @Override
    public String toString() {
        if (departures.isEmpty()) {
            return "No departures";
        }
        return String.format("%d departures (%s - %s)", tripCount, firstDeparture, lastDeparture);
    }
}