package tm.ugur.ugur_v3.domain.routeManagement.aggregate;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.geospatial.services.GeoSpatialService;
import tm.ugur.ugur_v3.domain.geospatial.valueobjects.Distance;
import tm.ugur.ugur_v3.domain.routeManagement.enums.RouteComplexity;
import tm.ugur.ugur_v3.domain.routeManagement.enums.RouteDirection;
import tm.ugur.ugur_v3.domain.routeManagement.enums.RouteStatus;
import tm.ugur.ugur_v3.domain.routeManagement.enums.RouteType;
import tm.ugur.ugur_v3.domain.routeManagement.events.*;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.EstimatedDuration;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.OperatingHours;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.entities.AggregateRoot;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;

import java.util.*;

@Getter
public class Route extends AggregateRoot<RouteId> {

    private static final int MAX_STOPS_PER_ROUTE = 100;
    private static final int MIN_STOPS_PER_ROUTE = 2;
    private static final double MAX_ROUTE_DISTANCE_KM = 150.0;
    private static final double MIN_ROUTE_DISTANCE_KM = 0.5;
    private static final int MAX_ROUTE_NAME_LENGTH = 100;
    private static final int MIN_ROUTE_NAME_LENGTH = 3;
    private static final double MIN_AVERAGE_SPEED_KMH = 8.0;
    private static final double MAX_AVERAGE_SPEED_KMH = 60.0;

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
    private Timestamp lastModifiedAt;

    private final Map<String, String> localizedNames;
    private final Map<String, String> localizedDescriptions;

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
        this.localizedNames = new HashMap<>();
        this.localizedDescriptions = new HashMap<>();

        this.status = RouteStatus.DRAFT;
        this.isActive = false;
        this.requiresMaintenanceAlert = false;
        this.totalDistance = Distance.zero();
        this.totalDuration = EstimatedDuration.zero();
        this.complexity = RouteComplexity.LOW;
        this.averageSpeed = 0.0;
        this.dailyRidership = 0;
        this.onTimePerformance = 100.0;
        this.lastPerformanceUpdate = Timestamp.now();
        this.lastModifiedAt = Timestamp.now();

        addDomainEvent(RouteCreatedEvent.of(
                routeId, routeName, routeType, direction, isCircular, createdBy, null
        ));
    }

    public void addStop(StopId stopId, GeoCoordinate stopLocation, String stopName,
                        GeoSpatialService geoSpatialService) {
        validateAddStop(stopId);

        if (stopSequence.isEmpty()) {
            stopSequence.add(stopId);
            publishStopAddedEvent(stopId, stopLocation, stopName, 0);
            return;
        }

        int newPosition = stopSequence.size();
        StopId previousStopId = stopSequence.get(newPosition - 1);

        Distance segmentDistance = calculateSegmentDistance(previousStopId, stopId, geoSpatialService);
        EstimatedDuration segmentTime = calculateEstimatedTime(segmentDistance);

        RouteSegment newSegment = RouteSegment.create(
                previousStopId, stopId, segmentDistance, segmentTime
        );

        segments.add(newSegment);
        stopSequence.add(stopId);

        recalculateRouteMetrics();
        updateComplexity();

        markAsModified();
        publishStopAddedEvent(stopId, stopLocation, stopName, newPosition);
    }

    private void recalculateRouteMetrics() {
        if (segments.isEmpty()) {
            this.totalDistance = Distance.zero();
            this.totalDuration = EstimatedDuration.zero();
            this.averageSpeed = 0.0;
            return;
        }

        this.totalDistance = segments.stream()
                .map(RouteSegment::getDistance)
                .reduce(Distance.zero(), Distance::add);

        this.totalDuration = segments.stream()
                .map(RouteSegment::getEstimatedTime)
                .reduce(EstimatedDuration.zero(), EstimatedDuration::add);

        if (totalDuration.getHours() > 0) {
            this.averageSpeed = totalDistance.toKilometers() / totalDuration.getHours();
        }

        validateRouteConstraints();
    }

    private void updateComplexity() {
        int stopsCount = stopSequence.size();
        double distanceKm = totalDistance.toKilometers();
        int highComplexitySegments = (int) segments.stream()
                .filter(RouteSegment::isHighTrafficComplexity)
                .count();

        int complexityScore = 0;

        if (stopsCount > 50) complexityScore += 3;
        else if (stopsCount > 20) complexityScore += 2;
        else if (stopsCount > 10) complexityScore += 1;

        if (distanceKm > 80) complexityScore += 3;
        else if (distanceKm > 40) complexityScore += 2;
        else if (distanceKm > 20) complexityScore += 1;

        if (highComplexitySegments > stopsCount * 0.5) complexityScore += 2;
        else if (highComplexitySegments > stopsCount * 0.25) complexityScore += 1;

        if (routeType == RouteType.EXPRESS) complexityScore += 1;
        if (isCircular) complexityScore += 1;

        this.complexity = determineComplexityLevel(complexityScore);
    }

    private RouteComplexity determineComplexityLevel(int score) {
        if (score >= 6) return RouteComplexity.HIGH;
        if (score >= 3) return RouteComplexity.MEDIUM;
        return RouteComplexity.LOW;
    }

    public void activate(OperatingHours operatingHours) {
        validateActivation();

        this.operatingHours = validateOperatingHours(operatingHours);
        this.status = RouteStatus.ACTIVE;
        this.isActive = true;
        this.lastModifiedAt = Timestamp.now();

        markAsModified();
        addDomainEvent(RouteActivatedEvent.of(getId(), operatingHours));
    }

    public void updatePerformanceMetrics(double newOnTimePerformance, int newDailyRidership) {
        if (newOnTimePerformance < 0 || newOnTimePerformance > 100) {
            throw new BusinessRuleViolationException(
                    "INVALID_PERFORMANCE_METRIC",
                    "On-time performance must be between 0 and 100"
            );
        }

        this.onTimePerformance = newOnTimePerformance;
        this.dailyRidership = Math.max(0, newDailyRidership);
        this.lastPerformanceUpdate = Timestamp.now();

        checkMaintenanceAlert();
        markAsModified();

        addDomainEvent(RoutePerformanceUpdatedEvent.of(
                getId(), onTimePerformance, dailyRidership
        ));
    }

    public Optional<StopId> getNextStop(StopId currentStopId) {
        int currentIndex = stopSequence.indexOf(currentStopId);
        if (currentIndex == -1 || currentIndex == stopSequence.size() - 1) {
            return isCircular && !stopSequence.isEmpty() ?
                    Optional.of(stopSequence.getFirst()) : Optional.empty();
        }
        return Optional.of(stopSequence.get(currentIndex + 1));
    }

    public Optional<RouteSegment> getSegmentBetween(StopId fromStop, StopId toStop) {
        return segments.stream()
                .filter(segment -> segment.getFromStopId().equals(fromStop) &&
                        segment.getToStopId().equals(toStop))
                .findFirst();
    }


    private Distance calculateSegmentDistance(StopId fromStopId,
                                              StopId toStopId,
                                              GeoSpatialService geoSpatialService) {
        // В реальной системе здесь будет запрос координат остановок
        // и расчет расстояния через GeoSpatialService

        // Временная заглушка с базовым расчетом
        return Distance.ofKilometers(2.5); // средняя дистанция между остановками
    }

    private EstimatedDuration calculateEstimatedTime(Distance distance) {
        double averageSpeedKmh = routeType == RouteType.EXPRESS ? 35.0 : 25.0;
        double hours = distance.toKilometers() / averageSpeedKmh;
        return EstimatedDuration.ofHours(hours);
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
        } else if (!needsAlert && requiresMaintenanceAlert) {
            this.requiresMaintenanceAlert = false;
            this.maintenanceNotes = null;
        }
    }

    // ================== VALIDATION METHODS ==================

    private String validateRouteName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessRuleViolationException("INVALID_ROUTE_NAME", "Route name cannot be empty");
        }
        if (name.length() < MIN_ROUTE_NAME_LENGTH || name.length() > MAX_ROUTE_NAME_LENGTH) {
            throw new BusinessRuleViolationException("INVALID_ROUTE_NAME",
                    "Route name must be between " + MIN_ROUTE_NAME_LENGTH + " and " + MAX_ROUTE_NAME_LENGTH + " characters");
        }
        return name.trim();
    }

    private String validateDescription(String description) {
        return description != null ? description.trim() : "";
    }

    private RouteType validateRouteType(RouteType type) {
        if (type == null) {
            throw new BusinessRuleViolationException("INVALID_ROUTE_TYPE", "Route type cannot be null");
        }
        return type;
    }

    private RouteDirection validateDirection(RouteDirection direction) {
        if (direction == null) {
            throw new BusinessRuleViolationException("INVALID_ROUTE_DIRECTION", "Route direction cannot be null");
        }
        return direction;
    }

    private int validateCapacity(int capacity) {
        if (capacity <= 0 || capacity > 200) {
            throw new BusinessRuleViolationException("INVALID_VEHICLE_CAPACITY",
                    "Vehicle capacity must be between 1 and 200");
        }
        return capacity;
    }

    private void validateAddStop(StopId stopId) {
        if (stopId == null) {
            throw new BusinessRuleViolationException("INVALID_STOP_ID", "Stop ID cannot be null");
        }
        if (stopSequence.contains(stopId)) {
            throw new BusinessRuleViolationException("STOP_ALREADY_EXISTS", "Stop already exists in route");
        }
        if (stopSequence.size() >= MAX_STOPS_PER_ROUTE) {
            throw new BusinessRuleViolationException("MAX_STOPS_EXCEEDED",
                    "Route cannot have more than " + MAX_STOPS_PER_ROUTE + " stops");
        }
    }

    private void validateActivation() {
        if (stopSequence.size() < MIN_STOPS_PER_ROUTE) {
            throw new BusinessRuleViolationException("INSUFFICIENT_STOPS",
                    "Route must have at least " + MIN_STOPS_PER_ROUTE + " stops to be activated");
        }
        if (status == RouteStatus.ARCHIVED) {
            throw new BusinessRuleViolationException("CANNOT_ACTIVATE_ARCHIVED",
                    "Cannot activate archived route");
        }
    }

    private OperatingHours validateOperatingHours(OperatingHours hours) {
        if (hours == null) {
            throw new BusinessRuleViolationException("INVALID_OPERATING_HOURS", "Operating hours cannot be null");
        }
        return hours;
    }

    private void validateRouteConstraints() {
        if (totalDistance.toKilometers() > MAX_ROUTE_DISTANCE_KM) {
            throw new BusinessRuleViolationException("ROUTE_TOO_LONG",
                    "Route distance cannot exceed " + MAX_ROUTE_DISTANCE_KM + " km");
        }
        if (averageSpeed < MIN_AVERAGE_SPEED_KMH || averageSpeed > MAX_AVERAGE_SPEED_KMH) {
            throw new BusinessRuleViolationException("INVALID_AVERAGE_SPEED",
                    "Average speed must be between " + MIN_AVERAGE_SPEED_KMH + " and " + MAX_AVERAGE_SPEED_KMH + " km/h");
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
            notes.append("Низкая пунктуальность: ").append(onTimePerformance).append("%. ");
        }
        if (averageSpeed < getMinExpectedSpeed()) {
            notes.append("Низкая средняя скорость: ").append(averageSpeed).append(" км/ч. ");
        }
        if (dailyRidership < getMinExpectedRidership()) {
            notes.append("Низкая загруженность: ").append(dailyRidership).append(" пассажиров. ");
        }
        return notes.toString();
    }

    private void publishStopAddedEvent(StopId stopId, GeoCoordinate location, String name, int position) {
        addDomainEvent(RouteStopAddedEvent.of(
                getId(), stopId, position, location, name
        ));
    }


    public void addLocalizedName(String languageCode, String localizedName) {
        if (languageCode != null && localizedName != null) {
            localizedNames.put(languageCode, localizedName.trim());
            markAsModified();
        }
    }

    public void addLocalizedDescription(String languageCode, String localizedDescription) {
        if (languageCode != null && localizedDescription != null) {
            localizedDescriptions.put(languageCode, localizedDescription.trim());
            markAsModified();
        }
    }

    public String getLocalizedName(String languageCode) {
        return localizedNames.getOrDefault(languageCode, routeName);
    }

    public String getLocalizedDescription(String languageCode) {
        return localizedDescriptions.getOrDefault(languageCode, routeDescription);
    }


    public boolean canAcceptMoreStops() {
        return stopSequence.size() < MAX_STOPS_PER_ROUTE;
    }

    public boolean isOperatingAt(Timestamp timestamp) {
        return isActive && operatingHours != null && operatingHours.isOperatingAt(timestamp);
    }

    public int getStopCount() {
        return stopSequence.size();
    }

    public List<StopId> getStopSequence() {
        return Collections.unmodifiableList(stopSequence);
    }

    public List<RouteSegment> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    @Override
    public String toString() {
        return String.format("Route{id=%s, name='%s', type=%s, stops=%d, distance=%s, status=%s}",
                getId(), routeName, routeType, stopSequence.size(), totalDistance, status);
    }
}