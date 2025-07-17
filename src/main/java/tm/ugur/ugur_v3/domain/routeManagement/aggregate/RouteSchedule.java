package tm.ugur.ugur_v3.domain.routeManagement.aggregate;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.enums.ScheduleType;
import tm.ugur.ugur_v3.domain.routeManagement.enums.ServiceFrequency;
import tm.ugur.ugur_v3.domain.routeManagement.events.*;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.*;
import tm.ugur.ugur_v3.domain.shared.entities.AggregateRoot;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class RouteSchedule extends AggregateRoot<RouteScheduleId> {

    private static final int MAX_TRIPS_PER_DAY = 500;
    private static final int MIN_HEADWAY_MINUTES = 1;
    private static final int MAX_HEADWAY_MINUTES = 240;
    private static final int MAX_SCHEDULE_PERIODS = 10;
    private static final int MAX_ADJUSTMENT_MINUTES = 30;
    private static final double MIN_SCHEDULE_ADHERENCE = 0.0;
    private static final double MAX_SCHEDULE_ADHERENCE = 100.0;

    private final RouteId routeId;
    private final String scheduleName;
    private final ScheduleType scheduleType;
    private final Timestamp effectiveFrom;
    private final Timestamp effectiveTo;

    private final Map<DayOfWeek, DailySchedule> dailySchedules;
    private final List<SchedulePeriod> schedulePeriods;
    private final Map<String, FrequencyPattern> frequencyPatterns;

    private final ServiceFrequency baseFrequency;
    private int totalDailyTrips;
    private EstimatedDuration totalServiceTime;
    private boolean isActive;

    private double scheduleAdherence;
    private int missedTrips;
    private double passengerLoadFactor;
    private Timestamp lastPerformanceUpdate;

    private final boolean allowsDynamicAdjustments;
    private final int maxDelayMinutes;
    private final int maxEarlyDepartureMinutes;
    private final Map<String, ScheduleAdjustment> activeAdjustments;


    public RouteSchedule(RouteScheduleId scheduleId, RouteId routeId, String scheduleName,
                         ScheduleType scheduleType, Timestamp effectiveFrom, Timestamp effectiveTo,
                         boolean allowsDynamicAdjustments, String createdBy) {
        super(scheduleId);

        this.routeId = validateRouteId(routeId);
        this.scheduleName = validateScheduleName(scheduleName);
        this.scheduleType = validateScheduleType(scheduleType);
        this.effectiveFrom = validateEffectiveFrom(effectiveFrom);
        this.effectiveTo = validateEffectiveTo(effectiveTo, effectiveFrom);
        this.allowsDynamicAdjustments = allowsDynamicAdjustments;

        this.dailySchedules = new EnumMap<>(DayOfWeek.class);
        this.schedulePeriods = new ArrayList<>();
        this.frequencyPatterns = new HashMap<>();
        this.activeAdjustments = new HashMap<>();

        this.baseFrequency = ServiceFrequency.MEDIUM;
        this.totalDailyTrips = 0;
        this.totalServiceTime = EstimatedDuration.zero();
        this.isActive = false;
        this.scheduleAdherence = 100.0;
        this.missedTrips = 0;
        this.passengerLoadFactor = 0.0;
        this.lastPerformanceUpdate = Timestamp.now();

        this.maxDelayMinutes = scheduleType == ScheduleType.FLEXIBLE ? 10 : 5;
        this.maxEarlyDepartureMinutes = scheduleType == ScheduleType.FLEXIBLE ? 2 : 1;

        addDomainEvent(RouteScheduleCreatedEvent.of(
                scheduleId, routeId, scheduleName, scheduleType, effectiveFrom, effectiveTo, createdBy
        ));
    }


    public void addDailySchedule(DayOfWeek dayOfWeek, DailySchedule dailySchedule) {
        validateDailySchedule(dayOfWeek, dailySchedule);

        dailySchedules.put(dayOfWeek, dailySchedule);
        recalculateTotalTrips();
        recalculateServiceTime();

        markAsModified();
       addDomainEvent(DailyScheduleAddedEvent.of(routeId, dayOfWeek, dailySchedule, "SYSTEM"));
    }

    public void addSchedulePeriod(SchedulePeriod period) {
        validateSchedulePeriod(period);

        if (schedulePeriods.size() >= MAX_SCHEDULE_PERIODS) {
            throw new BusinessRuleViolationException(
                    "MAX_SCHEDULE_PERIODS_EXCEEDED",
                    "Cannot add more than " + MAX_SCHEDULE_PERIODS + " schedule periods");
        }

        schedulePeriods.add(period);
        sortSchedulePeriods();

        markAsModified();
       addDomainEvent( SchedulePeriodAddedEvent.of(routeId, period, "SYSTEM"));
    }

    public void addFrequencyPattern(String patternName, FrequencyPattern pattern) {
        validateFrequencyPattern(patternName, pattern);

        frequencyPatterns.put(patternName, pattern);

        markAsModified();
        addDomainEvent(FrequencyPatternAddedEvent.standard(routeId, patternName, pattern, "SYSTEM"));
    }

    public void activate() {
        validateActivation();

        this.isActive = true;
        recalculateTotalTrips();
        recalculateServiceTime();

        markAsModified();
        addDomainEvent(RouteScheduleActivatedEvent.of(routeId, getId(), scheduleName,
                "SYSTEM", totalDailyTrips, dailySchedules.keySet()));
    }

    public void deactivate(String reason) {
        if (!isActive) {
            throw new BusinessRuleViolationException(
                    "SCHEDULE_NOT_ACTIVE",
                    "Schedule is already inactive");
        }

        this.isActive = false;
        clearActiveAdjustments();

        markAsModified();
        addDomainEvent(RouteScheduleDeactivatedEvent.permanent(routeId, getId(), scheduleName, "SYSTEM", reason));
    }


    public void applyDynamicAdjustment(DayOfWeek dayOfWeek, LocalTime time,
                                       int adjustmentMinutes, String reason, String adjustedBy) {
        validateDynamicAdjustment(dayOfWeek, time, adjustmentMinutes);

        String adjustmentKey = dayOfWeek + "_" + time.toString();
        ScheduleAdjustment adjustment = new ScheduleAdjustment(
                adjustmentKey, dayOfWeek, time, adjustmentMinutes, reason,
                adjustedBy, Timestamp.now()
        );

        activeAdjustments.put(adjustmentKey, adjustment);

        markAsModified();

        if (adjustmentMinutes > 0) {
            addDomainEvent(ScheduleDynamicallyAdjustedEvent.delay(
                    routeId, dayOfWeek, time, adjustmentMinutes, reason, adjustedBy));
        } else {
            addDomainEvent(ScheduleDynamicallyAdjustedEvent.advance(
                    routeId, dayOfWeek, time, Math.abs(adjustmentMinutes), reason, adjustedBy));
        }
    }

    public void removeDynamicAdjustment(DayOfWeek dayOfWeek, LocalTime time, String removedBy) {
        String adjustmentKey = dayOfWeek + "_" + time.toString();
        ScheduleAdjustment removed = activeAdjustments.remove(adjustmentKey);

        if (removed != null) {
            markAsModified();
            addDomainEvent(ScheduleAdjustmentRemovedEvent.manual(
                    routeId, dayOfWeek, time, removed.adjustmentMinutes(),
                    removed.id(), "Manual removal", removedBy));
        }
    }

    public void clearActiveAdjustments() {
        if (!activeAdjustments.isEmpty()) {
            int totalAdjustments = activeAdjustments.size();
            List<String> clearedAdjustmentIds = new ArrayList<>(activeAdjustments.keySet());

            activeAdjustments.clear();
            markAsModified();

            addDomainEvent(AllScheduleAdjustmentsClearedEvent.manual(
                    routeId, totalAdjustments, clearedAdjustmentIds,
                    "Manual clearance of all adjustments", "SYSTEM"));
        }
    }


    public Optional<LocalTime> getNextDeparture(DayOfWeek dayOfWeek, LocalTime fromTime) {
        DailySchedule dailySchedule = dailySchedules.get(dayOfWeek);
        if (dailySchedule == null || !isActive) {
            return Optional.empty();
        }

        LocalTime adjustedTime = applyAdjustmentsToTime(dayOfWeek, fromTime);
        return dailySchedule.getNextDeparture(adjustedTime);
    }

    public List<LocalTime> getDeparturesForDay(DayOfWeek dayOfWeek) {
        DailySchedule dailySchedule = dailySchedules.get(dayOfWeek);
        if (dailySchedule == null) {
            return Collections.emptyList();
        }

        List<LocalTime> baseDepartures = dailySchedule.getAllDepartures();
        return applyAdjustmentsToDepartures(dayOfWeek, baseDepartures);
    }

    public int getHeadwayAt(DayOfWeek dayOfWeek, LocalTime time) {
        for (SchedulePeriod period : schedulePeriods) {
            if (period.getOperatingDays().contains(dayOfWeek) &&
                    period.isTimeInPeriod(time)) {
                return period.getHeadwayMinutes();
            }
        }

        String patternName = getPatternNameForTime(dayOfWeek, time);
        FrequencyPattern pattern = frequencyPatterns.get(patternName);
        if (pattern != null) {
            return pattern.getHeadwayMinutes();
        }

        return baseFrequency.getDefaultHeadwayMinutes();
    }

    public boolean isEffectiveAt(Timestamp timestamp) {
        return isActive &&
                !timestamp.isBefore(effectiveFrom) &&
                (effectiveTo == null || !timestamp.isAfter(effectiveTo));
    }

    public int getTripsCountForDay(DayOfWeek dayOfWeek) {
        DailySchedule dailySchedule = dailySchedules.get(dayOfWeek);
        return dailySchedule != null ? dailySchedule.getTripCount() : 0;
    }

    public EstimatedDuration getServiceSpanForDay(DayOfWeek dayOfWeek) {
        DailySchedule dailySchedule = dailySchedules.get(dayOfWeek);
        if (dailySchedule == null) {
            return EstimatedDuration.zero();
        }

        LocalTime firstDeparture = dailySchedule.getFirstDeparture();
        LocalTime lastDeparture = dailySchedule.getLastDeparture();

        if (firstDeparture == null || lastDeparture == null) {
            return EstimatedDuration.zero();
        }

        long minutes = ChronoUnit.MINUTES.between(firstDeparture, lastDeparture);
        return EstimatedDuration.ofMinutes((int) minutes);
    }


    public void updatePerformanceMetrics(double newScheduleAdherence, int newMissedTrips,
                                         double newPassengerLoadFactor) {
        validatePerformanceMetrics(newScheduleAdherence, newMissedTrips, newPassengerLoadFactor);

        double previousAdherence = this.scheduleAdherence;
        int previousMissed = this.missedTrips;
        double previousLoadFactor = this.passengerLoadFactor;

        this.scheduleAdherence = newScheduleAdherence;
        this.missedTrips = newMissedTrips;
        this.passengerLoadFactor = newPassengerLoadFactor;
        this.lastPerformanceUpdate = Timestamp.now();

        analyzePerformanceAlerts();
        markAsModified();

        addDomainEvent(SchedulePerformanceUpdatedEvent.withComparison(
                routeId, scheduleAdherence, missedTrips, passengerLoadFactor,
                previousAdherence, previousMissed, previousLoadFactor));
    }

    private void analyzePerformanceAlerts() {
        List<String> alerts = new ArrayList<>();

        if (scheduleAdherence < 80.0) {
            alerts.add("Низкая пунктуальность: " + scheduleAdherence + "%");
        }

        if (missedTrips > totalDailyTrips * 0.05) {
            alerts.add("Высокий процент пропущенных рейсов: " + missedTrips);
        }

        if (passengerLoadFactor > 1.2) {
            alerts.add("Перегрузка автобусов: " + (passengerLoadFactor * 100) + "%");
        }

        if (!alerts.isEmpty()) {
            if (scheduleAdherence < 50.0) {
                addDomainEvent(SchedulePerformanceAlertEvent.lowAdherence(
                        routeId, scheduleAdherence, alerts));
            } else if (missedTrips > totalDailyTrips * 0.1) {
                addDomainEvent(SchedulePerformanceAlertEvent.excessiveMissedTrips(
                        routeId, missedTrips, alerts));
            } else if (passengerLoadFactor > 1.3) {
                addDomainEvent(SchedulePerformanceAlertEvent.overloadAlert(
                        routeId, passengerLoadFactor, alerts));
            } else {
                List<String> affectedMetrics = new ArrayList<>();
                if (scheduleAdherence < 80.0) affectedMetrics.add("schedule_adherence");
                if (missedTrips > totalDailyTrips * 0.05) affectedMetrics.add("missed_trips");
                if (passengerLoadFactor > 1.2) affectedMetrics.add("passenger_load_factor");

                List<String> recommendedActions = new ArrayList<>();
                recommendedActions.add("Review schedule feasibility");
                recommendedActions.add("Investigate operational issues");
                if (passengerLoadFactor > 1.2) recommendedActions.add("Consider frequency increase");

                addDomainEvent(SchedulePerformanceAlertEvent.systemicIssue(
                        routeId, alerts, affectedMetrics, recommendedActions));
            }
        }
    }


    private void recalculateTotalTrips() {
        this.totalDailyTrips = dailySchedules.values().stream()
                .mapToInt(DailySchedule::getTripCount)
                .max()
                .orElse(0);
    }

    private void recalculateServiceTime() {
        OptionalInt maxServiceMinutes = dailySchedules.keySet().stream()
                .mapToInt(dailySchedule -> (int) getServiceSpanForDay(dailySchedule).getMinutes())
                .max();

        this.totalServiceTime = maxServiceMinutes.isPresent() ?
                EstimatedDuration.ofMinutes(maxServiceMinutes.getAsInt()) :
                EstimatedDuration.zero();
    }

    private void sortSchedulePeriods() {
        schedulePeriods.sort(Comparator.comparing(SchedulePeriod::getStartTime));
    }

    private LocalTime applyAdjustmentsToTime(DayOfWeek dayOfWeek, LocalTime time) {
        String adjustmentKey = dayOfWeek + "_" + time.toString();
        ScheduleAdjustment adjustment = activeAdjustments.get(adjustmentKey);

        if (adjustment != null) {
            return time.plusMinutes(adjustment.adjustmentMinutes());
        }

        return time;
    }

    private List<LocalTime> applyAdjustmentsToDepartures(DayOfWeek dayOfWeek, List<LocalTime> departures) {
        return departures.stream()
                .map(time -> applyAdjustmentsToTime(dayOfWeek, time))
                .collect(Collectors.toList());
    }

    private String getPatternNameForTime(DayOfWeek dayOfWeek, LocalTime time) {
        if (time.isAfter(LocalTime.of(7, 0)) && time.isBefore(LocalTime.of(9, 0))) {
            return "MORNING_RUSH";
        }

        if (time.isAfter(LocalTime.of(17, 0)) && time.isBefore(LocalTime.of(19, 0))) {
            return "EVENING_RUSH";
        }

        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return "WEEKEND";
        }

        if (time.isAfter(LocalTime.of(22, 0)) || time.isBefore(LocalTime.of(6, 0))) {
            return "NIGHT";
        }

        return "REGULAR";
    }


    private RouteId validateRouteId(RouteId routeId) {
        if (routeId == null) {
            throw new BusinessRuleViolationException(
                    "INVALID_ROUTE_ID",
                    "Route ID cannot be null");
        }
        return routeId;
    }

    private String validateScheduleName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessRuleViolationException(
                    "INVALID_SCHEDULE_NAME",
                    "Schedule name cannot be empty");
        }
        if (name.length() > 100) {
            throw new BusinessRuleViolationException(
                    "SCHEDULE_NAME_TOO_LONG",
                    "Schedule name cannot exceed 100 characters");
        }
        return name.trim();
    }

    private ScheduleType validateScheduleType(ScheduleType type) {
        if (type == null) {
            throw new BusinessRuleViolationException(
                    "INVALID_SCHEDULE_TYPE",
                    "Schedule type cannot be null");
        }
        return type;
    }

    private Timestamp validateEffectiveFrom(Timestamp effectiveFrom) {
        if (effectiveFrom == null) {
            throw new BusinessRuleViolationException(
                    "INVALID_EFFECTIVE_FROM",
                    "Effective from date cannot be null");
        }
        return effectiveFrom;
    }

    private Timestamp validateEffectiveTo(Timestamp effectiveTo, Timestamp effectiveFrom) {
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new BusinessRuleViolationException(
                    "INVALID_EFFECTIVE_TO",
                    "Effective to date cannot be before effective from date");
        }
        return effectiveTo;
    }

    private void validateDailySchedule(DayOfWeek dayOfWeek, DailySchedule dailySchedule) {
        if (dayOfWeek == null) {
            throw new BusinessRuleViolationException(
                    "INVALID_DAY_OF_WEEK",
                    "Day of week cannot be null");
        }
        if (dailySchedule == null) {
            throw new BusinessRuleViolationException(
                    "INVALID_DAILY_SCHEDULE",
                    "Daily schedule cannot be null");
        }
        if (dailySchedule.getTripCount() > MAX_TRIPS_PER_DAY) {
            throw new BusinessRuleViolationException(
                    "TOO_MANY_TRIPS",
                    "Daily schedule cannot have more than " + MAX_TRIPS_PER_DAY + " trips");
        }
    }

    private void validateSchedulePeriod(SchedulePeriod period) {
        if (period == null) {
            throw new BusinessRuleViolationException(
                    "INVALID_SCHEDULE_PERIOD",
                    "Schedule period cannot be null");
        }
        if (period.getHeadwayMinutes() < MIN_HEADWAY_MINUTES ||
                period.getHeadwayMinutes() > MAX_HEADWAY_MINUTES) {
            throw new BusinessRuleViolationException(
                    "INVALID_HEADWAY",
                    "Headway must be between " + MIN_HEADWAY_MINUTES +
                            " and " + MAX_HEADWAY_MINUTES + " minutes");
        }

        for (SchedulePeriod existing : schedulePeriods) {
            if (period.overlapsWith(existing)) {
                throw new BusinessRuleViolationException(
                        "SCHEDULE_PERIOD_OVERLAP",
                        "Schedule period overlaps with existing period: " + existing.getName());
            }
        }
    }

    private void validateFrequencyPattern(String patternName, FrequencyPattern pattern) {
        if (patternName == null || patternName.trim().isEmpty()) {
            throw new BusinessRuleViolationException(
                    "INVALID_PATTERN_NAME",
                    "Pattern name cannot be empty");
        }
        if (pattern == null) {
            throw new BusinessRuleViolationException(
                    "INVALID_FREQUENCY_PATTERN",
                    "Frequency pattern cannot be null");
        }
        if (frequencyPatterns.containsKey(patternName)) {
            throw new BusinessRuleViolationException(
                    "PATTERN_ALREADY_EXISTS",
                    "Frequency pattern already exists: " + patternName);
        }
    }

    private void validateActivation() {
        if (dailySchedules.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "NO_DAILY_SCHEDULES",
                    "Cannot activate schedule without daily schedules");
        }
        if (isActive) {
            throw new BusinessRuleViolationException(
                    "SCHEDULE_ALREADY_ACTIVE",
                    "Schedule is already active");
        }
        if (!isEffectiveAt(Timestamp.now())) {
            throw new BusinessRuleViolationException(
                    "SCHEDULE_NOT_EFFECTIVE",
                    "Schedule is not effective at current time");
        }
    }

    private void validateDynamicAdjustment(DayOfWeek dayOfWeek, LocalTime time, int adjustmentMinutes) {
        if (!allowsDynamicAdjustments) {
            throw new BusinessRuleViolationException(
                    "DYNAMIC_ADJUSTMENTS_NOT_ALLOWED",
                    "Dynamic adjustments are not allowed for this schedule");
        }
        if (!isActive) {
            throw new BusinessRuleViolationException(
                    "SCHEDULE_NOT_ACTIVE",
                    "Cannot adjust inactive schedule");
        }
        if (Math.abs(adjustmentMinutes) > MAX_ADJUSTMENT_MINUTES) {
            throw new BusinessRuleViolationException(
                    "ADJUSTMENT_TOO_LARGE",
                    "Adjustment cannot exceed " + MAX_ADJUSTMENT_MINUTES + " minutes");
        }
        if (dayOfWeek == null || time == null) {
            throw new BusinessRuleViolationException(
                    "INVALID_ADJUSTMENT_TIME",
                    "Day of week and time cannot be null");
        }
    }

    private void validatePerformanceMetrics(double adherence, int missed, double loadFactor) {
        if (adherence < MIN_SCHEDULE_ADHERENCE || adherence > MAX_SCHEDULE_ADHERENCE) {
            throw new BusinessRuleViolationException(
                    "INVALID_SCHEDULE_ADHERENCE",
                    "Schedule adherence must be between " + MIN_SCHEDULE_ADHERENCE +
                            " and " + MAX_SCHEDULE_ADHERENCE);
        }
        if (missed < 0) {
            throw new BusinessRuleViolationException(
                    "INVALID_MISSED_TRIPS",
                    "Missed trips cannot be negative");
        }
        if (loadFactor < 0.0) {
            throw new BusinessRuleViolationException(
                    "INVALID_LOAD_FACTOR",
                    "Passenger load factor cannot be negative");
        }
    }


    public boolean hasScheduleForDay(DayOfWeek dayOfWeek) {
        return dailySchedules.containsKey(dayOfWeek);
    }

    public double getAverageHeadwayMinutes() {
        if (schedulePeriods.isEmpty()) {
            return baseFrequency.getDefaultHeadwayMinutes();
        }

        return schedulePeriods.stream()
                .mapToInt(SchedulePeriod::getHeadwayMinutes)
                .average()
                .orElse(baseFrequency.getDefaultHeadwayMinutes());
    }

    public int getPeakHeadwayMinutes() {
        return schedulePeriods.stream()
                .mapToInt(SchedulePeriod::getHeadwayMinutes)
                .min()
                .orElse(baseFrequency.getDefaultHeadwayMinutes());
    }

    public int getActiveDaysCount() {
        return dailySchedules.size();
    }

    public boolean hasActiveAdjustments() {
        return !activeAdjustments.isEmpty();
    }

    public List<ScheduleAdjustment> getActiveAdjustments() {
        return new ArrayList<>(activeAdjustments.values());
    }

    public boolean isPerformingWell() {
        return scheduleAdherence >= 85.0 &&
                missedTrips <= totalDailyTrips * 0.02 && // ≤2% пропущенных
                passengerLoadFactor <= 1.1; // ≤10% перегрузки
    }

    public boolean requiresAttention() {
        return scheduleAdherence < 75.0 ||
                missedTrips > totalDailyTrips * 0.05 ||
                passengerLoadFactor > 1.3;
    }


    public record ScheduleAdjustment(
                String id,
                DayOfWeek dayOfWeek,
                LocalTime time,
                int adjustmentMinutes,
                String reason,
                String adjustedBy,
                Timestamp createdAt) {

        public boolean isDelay() {
                return adjustmentMinutes > 0;
            }

            public boolean isEarlyDeparture() {
                return adjustmentMinutes < 0;
            }

            @Override
            public String toString() {
                String direction = adjustmentMinutes > 0 ? "delay" : "early";
                return String.format("%s %s: %d min %s (%s)",
                        dayOfWeek, time, Math.abs(adjustmentMinutes), direction, reason);
            }
        }


    @Override
    public String toString() {
        return String.format(
                "RouteSchedule{id=%s, route=%s, name='%s', type=%s, active=%s, days=%d, adherence=%.1f%%}",
                getId(), routeId, scheduleName, scheduleType, isActive,
                dailySchedules.size(), scheduleAdherence);
    }

    public String getDetailedInfo() {
        StringBuilder info = new StringBuilder();
        info.append(String.format("Schedule %s (%s)\n", scheduleName, getId()));
        info.append(String.format("Route: %s, Type: %s, Status: %s\n",
                routeId, scheduleType, isActive ? "Active" : "Inactive"));
        info.append(String.format("Effective: %s - %s\n",
                effectiveFrom, effectiveTo != null ? effectiveTo : "Indefinite"));
        info.append(String.format("Daily Trips: %d, Service Time: %s\n",
                totalDailyTrips, totalServiceTime));
        info.append(String.format("Performance: %.1f%% adherence, %d missed trips, %.1f load factor\n",
                scheduleAdherence, missedTrips, passengerLoadFactor));
        info.append(String.format("Active Days: %s\n",
                dailySchedules.keySet().toString()));

        if (hasActiveAdjustments()) {
            info.append(String.format("Active Adjustments: %d\n", activeAdjustments.size()));
        }

        return info.toString();
    }
}