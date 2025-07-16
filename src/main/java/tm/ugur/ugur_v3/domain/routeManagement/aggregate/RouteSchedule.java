package tm.ugur.ugur_v3.domain.routeManagement.aggregate;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.enums.ServiceFrequency;
import tm.ugur.ugur_v3.domain.routeManagement.events.*;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.*;
import tm.ugur.ugur_v3.domain.shared.entities.AggregateRoot;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;


import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

@Getter
public class RouteSchedule extends AggregateRoot<RouteScheduleId> {

    private static final int MAX_TRIPS_PER_DAY = 500;
    private static final int MIN_HEADWAY_MINUTES = 1;
    private static final int MAX_HEADWAY_MINUTES = 240;
    private static final int MAX_SCHEDULE_PERIODS = 10;

    private final RouteId routeId;
    private final String scheduleName;
    private final ScheduleType scheduleType;
    private final Timestamp effectiveFrom;
    private final Timestamp effectiveTo;

    private final Map<DayOfWeek, DailySchedule> dailySchedules;
    private final List<SchedulePeriod> schedulePeriods;
    private final Map<String, FrequencyPattern> frequencyPatterns;

    private ServiceFrequency baseFrequency;
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

    public RouteSchedule(RouteScheduleId scheduleId, Long version, Timestamp createdAt, Timestamp updatedAt,
                         RouteId routeId, String scheduleName, ScheduleType scheduleType,
                         Timestamp effectiveFrom, Timestamp effectiveTo, Map<DayOfWeek, DailySchedule> dailySchedules,
                         List<SchedulePeriod> schedulePeriods, Map<String, FrequencyPattern> frequencyPatterns,
                         ServiceFrequency baseFrequency, int totalDailyTrips, EstimatedDuration totalServiceTime,
                         boolean isActive, double scheduleAdherence, int missedTrips, double passengerLoadFactor,
                         boolean allowsDynamicAdjustments, int maxDelayMinutes, int maxEarlyDepartureMinutes) {
        super(scheduleId, version, createdAt, updatedAt);

        this.routeId = routeId;
        this.scheduleName = scheduleName;
        this.scheduleType = scheduleType;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.dailySchedules = dailySchedules != null ? new EnumMap<>(dailySchedules) : new EnumMap<>(DayOfWeek.class);
        this.schedulePeriods = schedulePeriods != null ? new ArrayList<>(schedulePeriods) : new ArrayList<>();
        this.frequencyPatterns = frequencyPatterns != null ? new HashMap<>(frequencyPatterns) : new HashMap<>();
        this.baseFrequency = baseFrequency != null ? baseFrequency : ServiceFrequency.MEDIUM;
        this.totalDailyTrips = totalDailyTrips;
        this.totalServiceTime = totalServiceTime != null ? totalServiceTime : EstimatedDuration.zero();
        this.isActive = isActive;
        this.scheduleAdherence = scheduleAdherence;
        this.missedTrips = missedTrips;
        this.passengerLoadFactor = passengerLoadFactor;
        this.allowsDynamicAdjustments = allowsDynamicAdjustments;
        this.maxDelayMinutes = maxDelayMinutes;
        this.maxEarlyDepartureMinutes = maxEarlyDepartureMinutes;
        this.lastPerformanceUpdate = Timestamp.now();
    }


    public void addDailySchedule(DayOfWeek dayOfWeek, DailySchedule dailySchedule, String modifiedBy) {
        validateCanModifySchedule();
        validateDailySchedule(dailySchedule);

        DailySchedule previousSchedule = dailySchedules.put(dayOfWeek, dailySchedule);

        recalculateScheduleMetrics();
        markAsModified();

        addDomainEvent(DailyScheduleAddedEvent.of(
                getId(), routeId, dayOfWeek, dailySchedule, previousSchedule != null, modifiedBy
        ));
    }

    public void addSchedulePeriod(String periodName, LocalTime startTime, LocalTime endTime,
                                  int headwayMinutes, Set<DayOfWeek> operatingDays, String addedBy) {
        validateCanModifySchedule();
        validateSchedulePeriod(periodName, startTime, endTime, headwayMinutes, operatingDays);

        if (schedulePeriods.size() >= MAX_SCHEDULE_PERIODS) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SCHEDULE_PERIODS_MAX",
                    "Maximum schedule periods exceeded: " + MAX_SCHEDULE_PERIODS);
        }

        SchedulePeriod period = SchedulePeriod.create(
                periodName, startTime, endTime, headwayMinutes, operatingDays
        );

        validateNoOverlappingPeriods(period);
        schedulePeriods.add(period);

        recalculateScheduleMetrics();
        markAsModified();

        addDomainEvent(SchedulePeriodAddedEvent.of(
                getId(), routeId, period, addedBy
        ));
    }

    public void setFrequencyPattern(String patternName, FrequencyPattern pattern, String modifiedBy) {
        validateCanModifySchedule();
        validateFrequencyPattern(pattern);

        FrequencyPattern previousPattern = frequencyPatterns.put(patternName, pattern);

        recalculateScheduleMetrics();
        markAsModified();

        addDomainEvent(FrequencyPatternUpdatedEvent.of(
                getId(), routeId, patternName, pattern, previousPattern != null, modifiedBy
        ));
    }

    public void activateSchedule(String activatedBy) {
        validateCanActivateSchedule();

        this.isActive = true;
        recalculateScheduleMetrics();
        markAsModified();

        addDomainEvent(RouteScheduleActivatedEvent.of(
                getId(), routeId, totalDailyTrips, baseFrequency, activatedBy
        ));
    }

    public void deactivateSchedule(String reason, String deactivatedBy) {
        if (!isActive) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SCHEDULE_NOT_ACTIVE",
                    "Schedule is already inactive");
        }

        this.isActive = false;
        markAsModified();

        addDomainEvent(RouteScheduleDeactivatedEvent.of(
                getId(), routeId, reason, deactivatedBy
        ));
    }

    public void updatePerformanceMetrics(double newScheduleAdherence, int newMissedTrips,
                                         double newPassengerLoadFactor) {
        validatePerformanceMetrics(newScheduleAdherence, newMissedTrips, newPassengerLoadFactor);

        double previousAdherence = this.scheduleAdherence;
        this.scheduleAdherence = newScheduleAdherence;
        this.missedTrips = newMissedTrips;
        this.passengerLoadFactor = newPassengerLoadFactor;
        this.lastPerformanceUpdate = Timestamp.now();

        markAsModified();

        addDomainEvent(SchedulePerformanceUpdatedEvent.of(
                getId(), routeId, newScheduleAdherence, previousAdherence, newMissedTrips, newPassengerLoadFactor
        ));

        if (newScheduleAdherence < 80.0 || newMissedTrips > 5) {
            addDomainEvent(SchedulePerformanceAlertEvent.of(
                    getId(), routeId, newScheduleAdherence, newMissedTrips, "Performance below threshold"
            ));
        }
    }

    public void adjustScheduleDynamically(DayOfWeek dayOfWeek, LocalTime time, int adjustmentMinutes,
                                          String reason, String adjustedBy) {
        if (!allowsDynamicAdjustments) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SCHEDULE_NOT_ALLOWED",
                    "Dynamic adjustments not allowed for this schedule");
        }

        if (Math.abs(adjustmentMinutes) > maxDelayMinutes) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SCHEDULE_ADJUSTMENT_MINUTES",
                    "Adjustment exceeds maximum allowed: " + adjustmentMinutes + " minutes"
            );
        }

        DailySchedule dailySchedule = dailySchedules.get(dayOfWeek);
        if (dailySchedule == null) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SCHEDULE_DAILY",
                    "No schedule found for " + dayOfWeek);
        }


        markAsModified();

        addDomainEvent(ScheduleDynamicallyAdjustedEvent.of(
                getId(), routeId, dayOfWeek, time, adjustmentMinutes, reason, adjustedBy
        ));
    }


    public Optional<LocalTime> getNextDeparture(DayOfWeek dayOfWeek, LocalTime fromTime) {
        DailySchedule dailySchedule = dailySchedules.get(dayOfWeek);
        if (dailySchedule == null || !isActive) {
            return Optional.empty();
        }

        return dailySchedule.getNextDeparture(fromTime);
    }

    public List<LocalTime> getDeparturesForDay(DayOfWeek dayOfWeek) {
        DailySchedule dailySchedule = dailySchedules.get(dayOfWeek);
        if (dailySchedule == null) {
            return Collections.emptyList();
        }

        return dailySchedule.getAllDepartures();
    }

    public int getHeadwayAt(DayOfWeek dayOfWeek, LocalTime time) {
        for (SchedulePeriod period : schedulePeriods) {
            if (period.getOperatingDays().contains(dayOfWeek) &&
                    period.isTimeInPeriod(time)) {
                return period.getHeadwayMinutes();
            }
        }

        return baseFrequency.getMinHeadwayMinutes();
    }

    public boolean isEffectiveAt(Timestamp timestamp) {
        return isActive &&
                !timestamp.isBefore(effectiveFrom) &&
                (effectiveTo == null || !timestamp.isAfter(effectiveTo));
    }

    public int getTripsCountForDay(DayOfWeek dayOfWeek) {
        DailySchedule dailySchedule = dailySchedules.get(dayOfWeek);
        if (dailySchedule == null) {
            return 0;
        }
        return dailySchedule.getTripCount();
    }

    public EstimatedDuration getServiceSpanForDay(DayOfWeek dayOfWeek) {
        DailySchedule dailySchedule = dailySchedules.get(dayOfWeek);
        if (dailySchedule == null) {
            return EstimatedDuration.zero();
        }
        return dailySchedule.getServiceSpan();
    }

    public boolean needsOptimization() {
        return scheduleAdherence < 85.0 ||
                missedTrips > 3 ||
                passengerLoadFactor > 120.0 ||
                passengerLoadFactor < 30.0;
    }

    public double getAverageHeadway() {
        if (schedulePeriods.isEmpty()) {
            return baseFrequency.getMinHeadwayMinutes();
        }

        return schedulePeriods.stream()
                .mapToInt(SchedulePeriod::getHeadwayMinutes)
                .average()
                .orElse(baseFrequency.getMinHeadwayMinutes());
    }


    private RouteId validateRouteId(RouteId routeId) {
        if (routeId == null) {
            throw new BusinessRuleViolationException("ROUTE_SCHEDULE_ID", "Route ID cannot be null");
        }
        return routeId;
    }

    private String validateScheduleName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessRuleViolationException("ROUTE_SCHEDULE_NAME", "Schedule name cannot be empty");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 100) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SCHEDULE_LENGTH", "Schedule name too long: " + trimmed.length());
        }
        return trimmed;
    }

    private ScheduleType validateScheduleType(ScheduleType type) {
        if (type == null) {
            throw new BusinessRuleViolationException("ROUTE_SCHEDULE_TYPE", "Schedule type cannot be null");
        }
        return type;
    }

    private Timestamp validateEffectiveFrom(Timestamp effectiveFrom) {
        if (effectiveFrom == null) {
            throw new BusinessRuleViolationException("ROUTE_SCHEDULE_EFFECTIVE", "Effective from date cannot be null");
        }
        return effectiveFrom;
    }

    private Timestamp validateEffectiveTo(Timestamp effectiveTo, Timestamp effectiveFrom) {
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SCHEDULE_EFFECTIVE",
                    "Effective to date cannot be before effective from date");
        }
        return effectiveTo;
    }

    private void validateCanModifySchedule() {
        if (isActive && !allowsDynamicAdjustments) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SCHEDULE_MODIFY",
                    "Cannot modify active schedule without dynamic adjustment capability");
        }
    }

    private void validateCanActivateSchedule() {
        if (isActive) {
            throw new BusinessRuleViolationException("ROUTE_SCHEDULE_ACTIVATE", "Schedule is already active");
        }
        if (dailySchedules.isEmpty() && schedulePeriods.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SCHEDULE_EMPTY",
                    "Cannot activate schedule without daily schedules or periods");
        }
    }

    private void validateDailySchedule(DailySchedule dailySchedule) {
        if (dailySchedule == null) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SCHEDULE_DAILY",
                    "Daily schedule cannot be null");
        }
        if (dailySchedule.getTripCount() > MAX_TRIPS_PER_DAY) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SCHEDULE_DAILY_MAX",
                    "Too many trips in daily schedule: " + dailySchedule.getTripCount() + ", max: " + MAX_TRIPS_PER_DAY
            );
        }
    }

    private void validateSchedulePeriod(String periodName, LocalTime startTime, LocalTime endTime,
                                        int headwayMinutes, Set<DayOfWeek> operatingDays) {
        if (periodName == null || periodName.trim().isEmpty()) {
            throw new BusinessRuleViolationException("ROUTE_SCHEDULE_PERIOD", "Period name cannot be empty");
        }
        if (startTime == null || endTime == null) {
            throw new BusinessRuleViolationException("ROUTE_SCHEDULE_TIME", "Start and end times cannot be null");
        }
        if (headwayMinutes < MIN_HEADWAY_MINUTES || headwayMinutes > MAX_HEADWAY_MINUTES) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SCHEDULE_HEADWAY_MINUTES",
                    "Headway must be between " + MIN_HEADWAY_MINUTES + " and " + MAX_HEADWAY_MINUTES + " minutes"
            );
        }
        if (operatingDays == null || operatingDays.isEmpty()) {
            throw new BusinessRuleViolationException("ROUTE_SCHEDULE_DAY", "Operating days cannot be empty");
        }
    }

    private void validateNoOverlappingPeriods(SchedulePeriod newPeriod) {
        for (SchedulePeriod existingPeriod : schedulePeriods) {
            if (existingPeriod.overlapsWith(newPeriod)) {
                throw new BusinessRuleViolationException(
                        "ROUTE_SCHEDULE_PERIOD",
                        "Schedule period overlaps with existing period: " + existingPeriod.getPeriodName()
                );
            }
        }
    }

    private void validateFrequencyPattern(FrequencyPattern pattern) {
        if (pattern == null) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SCHEDULE_FREQUENCY",
                    "Frequency pattern cannot be null");
        }
        // Additional validation logic can be added here
    }

    private void validatePerformanceMetrics(double adherence, int missedTrips, double loadFactor) {
        if (adherence < 0 || adherence > 100) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SCHEDULE_PERFORMANCE_METRICS",
                    "Schedule adherence must be between 0 and 100");
        }
        if (missedTrips < 0) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SCHEDULE_MISSED_TRIPS",
                    "Missed trips cannot be negative");
        }
        if (loadFactor < 0) {
            throw new BusinessRuleViolationException("ROUTE_SCHEDULE_FACTOR", "Load factor cannot be negative");
        }
    }


    private void recalculateScheduleMetrics() {
        // Calculate total daily trips
        this.totalDailyTrips = dailySchedules.values().stream()
                .mapToInt(DailySchedule::getTripCount)
                .max()
                .orElse(0);

        // Calculate total service time
        this.totalServiceTime = dailySchedules.values().stream()
                .map(DailySchedule::getServiceSpan)
                .max(Comparator.comparing(EstimatedDuration::getSeconds))
                .orElse(EstimatedDuration.zero());

        // Update base frequency based on schedule periods
        if (!schedulePeriods.isEmpty()) {
            double avgHeadway = getAverageHeadway();
            this.baseFrequency = ServiceFrequency.fromHeadway((int) avgHeadway);
        }
    }


    public boolean operatesOnDay(DayOfWeek dayOfWeek) {
        return dailySchedules.containsKey(dayOfWeek) ||
                schedulePeriods.stream().anyMatch(p -> p.getOperatingDays().contains(dayOfWeek));
    }

    public Set<DayOfWeek> getOperatingDays() {
        Set<DayOfWeek> operatingDays = EnumSet.copyOf(dailySchedules.keySet());
        schedulePeriods.forEach(period -> operatingDays.addAll(period.getOperatingDays()));
        return operatingDays;
    }

    public boolean isHighFrequency() {
        return baseFrequency.isHighFrequency();
    }

    public boolean isLowFrequency() {
        return baseFrequency.isLowFrequency();
    }

    public boolean hasGoodPerformance() {
        return scheduleAdherence >= 90.0 && missedTrips <= 2;
    }

    public boolean isFlexible() {
        return allowsDynamicAdjustments;
    }

    public Map<DayOfWeek, DailySchedule> getDailySchedulesView() {
        return Collections.unmodifiableMap(dailySchedules);
    }

    public List<SchedulePeriod> getSchedulePeriodsView() {
        return Collections.unmodifiableList(schedulePeriods);
    }

    public Map<String, FrequencyPattern> getFrequencyPatternsView() {
        return Collections.unmodifiableMap(frequencyPatterns);
    }


    @Getter
    public enum ScheduleType {
        FIXED("Fixed Schedule", "Traditional fixed timetable"),
        FLEXIBLE("Flexible Schedule", "Allows dynamic adjustments"),
        FREQUENCY_BASED("Frequency Based", "Based on headway rather than fixed times"),
        DEMAND_RESPONSIVE("Demand Responsive", "Adjusts based on passenger demand"),
        HYBRID("Hybrid", "Combination of fixed and flexible elements");

        private final String displayName;
        private final String description;

        ScheduleType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public boolean allowsDynamicChanges() {
            return this != FIXED;
        }

        public boolean isFixedTimetable() {
            return this == FIXED || this == HYBRID;
        }

        public boolean isFrequencyBased() {
            return this == FREQUENCY_BASED || this == DEMAND_RESPONSIVE;
        }
    }
}