package tm.ugur.ugur_v3.domain.routeManagement.aggregate;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.enums.RouteDirection;
import tm.ugur.ugur_v3.domain.routeManagement.enums.RouteStatus;
import tm.ugur.ugur_v3.domain.routeManagement.enums.RouteType;
import tm.ugur.ugur_v3.domain.routeManagement.events.*;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.*;
import tm.ugur.ugur_v3.domain.shared.entities.AggregateRoot;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
public class Route extends AggregateRoot<RouteId> {

    private static final int MAX_STOPS_PER_ROUTE = 100;
    private static final int MIN_STOPS_PER_ROUTE = 2;
    private static final double MAX_ROUTE_DISTANCE_KM = 150.0;
    private static final double MIN_ROUTE_DISTANCE_KM = 0.5;
    private static final int MAX_ROUTE_NAME_LENGTH = 100;
    private static final int MIN_ROUTE_NAME_LENGTH = 3;

    private final String routeName;
    private final String routeDescription;
    private final RouteType routeType;
    private final RouteDirection direction;
    private RouteStatus status;

    private final List<RouteSegment> segments;
    private final List<StopId> stopSequence;
    private Distance totalDistance;
    private EstimatedDuration totalDuration;

    private OperatingHours operatingHours;
    private RouteComplexity complexity;
    private final boolean isCircular;
    private final int vehicleCapacityRequired;

    private double averageSpeed;
    private int dailyRidership;
    private double onTimePerformance;
    private Timestamp lastPerformanceUpdate;

    private boolean isActive;
    private boolean requiresMaintenanceAlert;
    private String maintenanceNotes;
    private Timestamp lastModifiedBy;

    public Route(RouteId routeId, String routeName, String routeDescription,
                 RouteType routeType, RouteDirection direction, boolean isCircular,
                 int vehicleCapacityRequired, String createdBy) {
        super(routeId);


        this.routeName = validateRouteName(routeName);
        this.routeDescription = validateDescription(routeDescription);
        this.routeType = validateRouteType(routeType);
        this.direction = validateDirection(direction);
        this.isCircular = isCircular;
        this.vehicleCapacityRequired = validateCapacity(vehicleCapacityRequired);

        this.segments = new ArrayList<>();
        this.stopSequence = new ArrayList<>();

        this.status = RouteStatus.DRAFT;
        this.isActive = false;
        this.requiresMaintenanceAlert = false;
        this.totalDistance = Distance.zero();
        this.totalDuration = EstimatedDuration.zero();
        this.averageSpeed = 0.0;
        this.dailyRidership = 0;
        this.onTimePerformance = 0.0;
        this.lastPerformanceUpdate = Timestamp.now();
        this.lastModifiedBy = Timestamp.now();


        addDomainEvent(RouteCreatedEvent.of(
                routeId,
                routeName,
                routeType,
                direction,
                isCircular,
                createdBy,
                null
        ));
    }

    public Route(RouteId routeId, Long version, Timestamp createdAt, Timestamp updatedAt,
                 String routeName, String routeDescription, RouteType routeType,
                 RouteDirection direction, RouteStatus status, List<RouteSegment> segments,
                 List<StopId> stopSequence, Distance totalDistance, EstimatedDuration totalDuration,
                 OperatingHours operatingHours, RouteComplexity complexity, boolean isCircular,
                 int vehicleCapacityRequired, double averageSpeed, int dailyRidership,
                 double onTimePerformance, boolean isActive, boolean requiresMaintenanceAlert,
                 String maintenanceNotes, Timestamp lastPerformanceUpdate) {
        super(routeId, version, createdAt, updatedAt);

        this.routeName = routeName;
        this.routeDescription = routeDescription;
        this.routeType = routeType;
        this.direction = direction;
        this.status = status;
        this.segments = segments != null ? new ArrayList<>(segments) : new ArrayList<>();
        this.stopSequence = stopSequence != null ? new ArrayList<>(stopSequence) : new ArrayList<>();
        this.totalDistance = totalDistance != null ? totalDistance : Distance.zero();
        this.totalDuration = totalDuration != null ? totalDuration : EstimatedDuration.zero();
        this.operatingHours = operatingHours;
        this.complexity = complexity;
        this.isCircular = isCircular;
        this.vehicleCapacityRequired = vehicleCapacityRequired;
        this.averageSpeed = averageSpeed;
        this.dailyRidership = dailyRidership;
        this.onTimePerformance = onTimePerformance;
        this.isActive = isActive;
        this.requiresMaintenanceAlert = requiresMaintenanceAlert;
        this.maintenanceNotes = maintenanceNotes;
        this.lastPerformanceUpdate = lastPerformanceUpdate != null ? lastPerformanceUpdate : Timestamp.now();
    }


    public void addStop(StopId stopId, int position, Distance distanceFromPrevious) {
        validateCanModifyRoute();
        validateStopPosition(position);
        validateStopNotAlreadyExists(stopId);

        if (stopSequence.size() >= MAX_STOPS_PER_ROUTE) {
            throw new BusinessRuleViolationException(
                    "ROUTE_STOPS_PER_ROUTE EXCEEDED",
                    "Route cannot have more than " + MAX_STOPS_PER_ROUTE + " stops"
            );
        }

        stopSequence.add(position, stopId);

        if (position > 0) {
            RouteSegment segment = RouteSegment.create(
                    stopSequence.get(position - 1),
                    stopId,
                    distanceFromPrevious,
                    calculateEstimatedTime(distanceFromPrevious)
            );
            segments.add(position - 1, segment);
        }

        recalculateRouteMetrics();
        markAsModified();

        addDomainEvent(RouteStopAddedEvent.of(
                    getId(), stopId, position, distanceFromPrevious, stopSequence.size()
        ));
    }

    public void removeStop(StopId stopId, String reason, String modifiedBy) {
        validateCanModifyRoute();

        int position = stopSequence.indexOf(stopId);
        if (position == -1) {
            throw new BusinessRuleViolationException(
                    "ROUTE_STOP_REMOVED",
                    "Stop not found in route: " + stopId);
        }

        if (stopSequence.size() <= MIN_STOPS_PER_ROUTE) {
            throw new BusinessRuleViolationException(
                    "ROUTE_STOP_MIN_STOPS_PER_ROUTE",
                    "Route must have at least " + MIN_STOPS_PER_ROUTE + " stops"
            );
        }

        stopSequence.remove(position);

        if (position > 0 && position < segments.size()) {
            segments.remove(position - 1);
            if (position < stopSequence.size()) {
                mergeSegments(position - 1);
            }
        } else if (position < segments.size()) {
            segments.remove(position);
        }

        recalculateRouteMetrics();
        markAsModified();

        addDomainEvent(RouteStopRemovedEvent.of(
                getId(), stopId, position, reason, modifiedBy, stopSequence.size()
        ));
    }

    public void activateRoute(OperatingHours operatingHours, String activatedBy) {
        validateCanActivateRoute();

        this.operatingHours = validateOperatingHours(operatingHours);
        this.status = RouteStatus.ACTIVE;
        this.isActive = true;
        this.complexity = calculateRouteComplexity();

        recalculateRouteMetrics();
        markAsModified();

        addDomainEvent(RouteActivatedEvent.of(
                getId(), operatingHours, complexity, totalDistance,
                stopSequence.size(), activatedBy
        ));
    }

    public void deactivateRoute(String reason, String deactivatedBy) {
        if (!isActive) {
            throw new BusinessRuleViolationException(
                    "ROUTE_NOT_ACTIVE",
                    "Route is already inactive");
        }

        this.status = RouteStatus.INACTIVE;
        this.isActive = false;
        markAsModified();

        addDomainEvent(RouteDeactivatedEvent.of(
                getId(), reason, deactivatedBy, getActiveVehicleCount()
        ));
    }

    public void updatePerformanceMetrics(double newAverageSpeed, int newDailyRidership,
                                         double newOnTimePerformance) {
        if (newAverageSpeed < 0 || newAverageSpeed > 200) {
            throw new BusinessRuleViolationException(
                    "ROUTE_AVERAGE_SPEED",
                    "Invalid average speed: " + newAverageSpeed);
        }

        if (newDailyRidership < 0) {
            throw new BusinessRuleViolationException(
                    "ROUTE_DAILY_RIDERSHIP",
                    "Invalid ridership: " + newDailyRidership);
        }

        if (newOnTimePerformance < 0 || newOnTimePerformance > 100) {
            throw new BusinessRuleViolationException(
                    "ROUTE_ON_TIME_PERFORMANCE",
                    "Invalid on-time performance: " + newOnTimePerformance);
        }

        this.averageSpeed = newAverageSpeed;
        this.dailyRidership = newDailyRidership;
        this.onTimePerformance = newOnTimePerformance;
        this.lastPerformanceUpdate = Timestamp.now();

        checkMaintenanceAlert();
        markAsModified();

        addDomainEvent(RoutePerformanceUpdatedEvent.of(
                getId(), newAverageSpeed, newDailyRidership, newOnTimePerformance
        ));
    }


    public EstimatedDuration getEstimatedTimeBetweenStops(StopId fromStop, StopId toStop) {
        int fromIndex = stopSequence.indexOf(fromStop);
        int toIndex = stopSequence.indexOf(toStop);

        if (fromIndex == -1 || toIndex == -1 || fromIndex >= toIndex) {
            throw new BusinessRuleViolationException(
                    "ROUTE_STOP_INVALID",
                    "Invalid stop sequence for time calculation");
        }

        return segments.subList(fromIndex, toIndex)
                .stream()
                .map(RouteSegment::getEstimatedTime)
                .reduce(EstimatedDuration.zero(), EstimatedDuration::add);
    }

    public Distance getDistanceBetweenStops(StopId fromStop, StopId toStop) {
        int fromIndex = stopSequence.indexOf(fromStop);
        int toIndex = stopSequence.indexOf(toStop);

        if (fromIndex == -1 || toIndex == -1 || fromIndex >= toIndex) {
            throw new BusinessRuleViolationException(
                    "ROUTE_STOP_INVALID",
                    "Invalid stop sequence for distance calculation");
        }

        return segments.subList(fromIndex, toIndex)
                .stream()
                .map(RouteSegment::getDistance)
                .reduce(Distance.zero(), Distance::add);
    }

    public boolean isOperatingAt(Timestamp time) {
        return isActive &&
                operatingHours != null &&
                operatingHours.isOperatingAt(time);
    }

    public Optional<StopId> getNextStop(StopId currentStop) {
        int currentIndex = stopSequence.indexOf(currentStop);
        if (currentIndex == -1 || currentIndex >= stopSequence.size() - 1) {
            return isCircular ? Optional.of(stopSequence.getFirst()) : Optional.empty();
        }
        return Optional.of(stopSequence.get(currentIndex + 1));
    }

    public boolean needsMaintenance() {
        return requiresMaintenanceAlert ||
                onTimePerformance < 70.0 ||
                (averageSpeed > 0 && averageSpeed < getMinExpectedSpeed());
    }


    private String validateRouteName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessRuleViolationException(
                    "ROUTE_NAME_INVALID",
                    "Route name cannot be empty");
        }
        String trimmed = name.trim();
        if (trimmed.length() < MIN_ROUTE_NAME_LENGTH || trimmed.length() > MAX_ROUTE_NAME_LENGTH) {
            throw new BusinessRuleViolationException(
                    "ROUTE_NAME_INVALID",
                    "Route name must be between " + MIN_ROUTE_NAME_LENGTH +
                            " and " + MAX_ROUTE_NAME_LENGTH + " characters"
            );
        }
        return trimmed;
    }

    private String validateDescription(String description) {
        if (description != null && description.length() > 500) {
            throw new BusinessRuleViolationException("ROUTE_INVALID_DESCRIPTION", "Route description too long");
        }
        return description != null ? description.trim() : "";
    }

    private RouteType validateRouteType(RouteType type) {
        if (type == null) {
            throw new BusinessRuleViolationException("ROUTE_INVALID_TYPE", "Route type cannot be null");
        }
        return type;
    }

    private RouteDirection validateDirection(RouteDirection direction) {
        if (direction == null) {
            throw new BusinessRuleViolationException("ROUTE_INVALID_DIRECTION", "Route direction cannot be null");
        }
        return direction;
    }

    private int validateCapacity(int capacity) {
        if (capacity < 10 || capacity > 300) {
            throw new BusinessRuleViolationException("ROUTE_INVALID_CAPACITY",
                    "Vehicle capacity must be between 10 and 300");
        }
        return capacity;
    }

    private OperatingHours validateOperatingHours(OperatingHours hours) {
        if (hours == null) {
            throw new BusinessRuleViolationException("ROUTE_INVALID_OPERATING_HOURS",
                    "Operating hours cannot be null for active route");
        }
        return hours;
    }

    private void validateCanModifyRoute() {
        if (status == RouteStatus.ARCHIVED) {
            throw new BusinessRuleViolationException("ROUTE_INVALID_MODIFY", "Cannot modify archived route");
        }
    }

    private void validateCanActivateRoute() {
        if (stopSequence.size() < MIN_STOPS_PER_ROUTE) {
            throw new BusinessRuleViolationException(
                    "ROUTE_INVALID_ACTIVATE",
                    "Route must have at least " + MIN_STOPS_PER_ROUTE + " stops to be activated"
            );
        }
        if (status == RouteStatus.ARCHIVED) {
            throw new BusinessRuleViolationException(
                    "ROUTE_INVALID_ARCHIVE",
                    "Cannot activate archived route");
        }
    }

    private void validateStopPosition(int position) {
        if (position < 0 || position > stopSequence.size()) {
            throw new BusinessRuleViolationException(
                    "ROUTE_INVALID_OPERATING_HOUR",
                    "Invalid stop position: " + position);
        }
    }

    private void validateStopNotAlreadyExists(StopId stopId) {
        if (stopSequence.contains(stopId)) {
            throw new BusinessRuleViolationException(
                    "ROUTE_INVALID_EXISTS",
                    "Stop already exists in route: " + stopId);
        }
    }


    private void recalculateRouteMetrics() {
        this.totalDistance = segments.stream()
                .map(RouteSegment::getDistance)
                .reduce(Distance.zero(), Distance::add);

        this.totalDuration = segments.stream()
                .map(RouteSegment::getEstimatedTime)
                .reduce(EstimatedDuration.zero(), EstimatedDuration::add);
    }

    private RouteComplexity calculateRouteComplexity() {
        int stops = stopSequence.size();
        double distanceKm = totalDistance.getKilometers();

        if (stops > 50 || distanceKm > 80) return RouteComplexity.HIGH;
        if (stops > 20 || distanceKm > 30) return RouteComplexity.MEDIUM;
        return RouteComplexity.LOW;
    }

    private EstimatedDuration calculateEstimatedTime(Distance distance) {
        double hours = distance.getKilometers() / 30.0;
        return EstimatedDuration.ofHours(hours);
    }

    private void mergeSegments(int segmentIndex) {
        if (segmentIndex < 0 || segmentIndex >= segments.size() - 1) return;

        RouteSegment first = segments.get(segmentIndex);
        RouteSegment second = segments.get(segmentIndex + 1);

        Distance combinedDistance = first.getDistance().add(second.getDistance());
        EstimatedDuration combinedTime = first.getEstimatedTime().add(second.getEstimatedTime());

        RouteSegment merged = RouteSegment.create(
                first.getFromStopId(),
                second.getToStopId(),
                combinedDistance,
                combinedTime
        );

        segments.set(segmentIndex, merged);
        segments.remove(segmentIndex + 1);
    }

    private void checkMaintenanceAlert() {
        boolean needsAlert = onTimePerformance < 70.0 ||
                averageSpeed < getMinExpectedSpeed() ||
                dailyRidership < getMinExpectedRidership();

        if (needsAlert && !requiresMaintenanceAlert) {
            this.requiresMaintenanceAlert = true;
            this.maintenanceNotes = generateMaintenanceNotes();

            addDomainEvent(RouteMaintenanceAlertEvent.of(
                    getId(), maintenanceNotes, onTimePerformance, averageSpeed
            ));
        }
    }

    private double getMinExpectedSpeed() {
        return routeType == RouteType.EXPRESS ? 25.0 : 15.0;
    }

    private int getMinExpectedRidership() {
        return routeType == RouteType.EXPRESS ? 500 : 200;
    }

    private String generateMaintenanceNotes() {
        StringBuilder notes = new StringBuilder();
        if (onTimePerformance < 70.0) {
            notes.append("Low on-time performance: ").append(onTimePerformance).append("%. ");
        }
        if (averageSpeed < getMinExpectedSpeed()) {
            notes.append("Low average speed: ").append(averageSpeed).append(" km/h. ");
        }
        if (dailyRidership < getMinExpectedRidership()) {
            notes.append("Low ridership: ").append(dailyRidership).append(" passengers. ");
        }
        return notes.toString();
    }

    private int getActiveVehicleCount() {
        // This would be calculated from vehicle assignments
        // For now, return estimated based on route complexity
        return complexity == RouteComplexity.HIGH ? 8 :
                complexity == RouteComplexity.MEDIUM ? 4 : 2;
    }


    public List<StopId> getStopSequenceView() {
        return Collections.unmodifiableList(stopSequence);
    }

    public List<RouteSegment> getSegmentsView() {
        return Collections.unmodifiableList(segments);
    }

    public boolean hasStop(StopId stopId) {
        return stopSequence.contains(stopId);
    }

    public int getStopCount() {
        return stopSequence.size();
    }

    public boolean isReadyForService() {
        return stopSequence.size() >= MIN_STOPS_PER_ROUTE &&
                operatingHours != null &&
                !segments.isEmpty();
    }
}