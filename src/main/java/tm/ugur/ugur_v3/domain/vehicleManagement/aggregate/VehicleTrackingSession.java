package tm.ugur.ugur_v3.domain.vehicleManagement.aggregate;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.entities.AggregateRoot;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.Speed;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.Bearing;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.TrackingSessionId;
import tm.ugur.ugur_v3.domain.vehicleManagement.events.TrackingSessionStartedEvent;
import tm.ugur.ugur_v3.domain.vehicleManagement.events.TrackingSessionEndedEvent;
import tm.ugur.ugur_v3.domain.vehicleManagement.events.GPSDataReceivedEvent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class VehicleTrackingSession extends AggregateRoot<TrackingSessionId> {

    private static final long MAX_SESSION_DURATION_HOURS = 24;
    private static final double MIN_GPS_ACCURACY_METERS = 100.0;
    private static final long MAX_GPS_DATA_AGE_MINUTES = 10;
    private static final int MAX_GPS_POINTS_IN_MEMORY = 1000;

    private final VehicleId vehicleId;
    private final String routeId;
    private final String driverId;
    private SessionStatus status;
    private Timestamp startTime;
    private Timestamp endTime;

    private GeoCoordinate currentLocation;
    private GeoCoordinate startLocation;
    private Speed currentSpeed;
    private Bearing currentBearing;
    private Timestamp lastGpsUpdate;
    private double totalDistanceTraveled;
    private int gpsPointsReceived;
    private int validGpsPointsCount;
    private int filteredGpsPointsCount;

    private double averageSpeed;
    private Speed maxSpeed;
    private final long totalStoppedTimeMinutes;
    private double gpsAccuracyPercentage;

    private final List<GPSDataPoint> recentGpsPoints;

    public VehicleTrackingSession(TrackingSessionId sessionId, VehicleId vehicleId,
                                  String routeId, String driverId) {
        super(sessionId);

        this.vehicleId = validateVehicleId(vehicleId);
        this.routeId = validateRouteId(routeId);
        this.driverId = driverId;
        this.status = SessionStatus.CREATED;
        this.totalDistanceTraveled = 0.0;
        this.gpsPointsReceived = 0;
        this.validGpsPointsCount = 0;
        this.filteredGpsPointsCount = 0;
        this.totalStoppedTimeMinutes = 0L;
        this.gpsAccuracyPercentage = 0.0;
        this.recentGpsPoints = new ArrayList<>();
    }

    public void startSession(GeoCoordinate initialLocation, String startedBy) {
        validateCanStart();

        this.status = SessionStatus.ACTIVE;
        this.startTime = Timestamp.now();
        this.startLocation = initialLocation;
        this.currentLocation = initialLocation;
        this.lastGpsUpdate = startTime;
        this.currentSpeed = Speed.zero();
        this.currentBearing = Bearing.north();
        this.maxSpeed = Speed.zero();

        markAsModified();
        addDomainEvent(TrackingSessionStartedEvent.of(
                getId(), vehicleId, routeId, initialLocation, startedBy
        ));
    }

    public void processGPSData(GeoCoordinate location, Speed speed, Bearing bearing,
                               Timestamp gpsTimestamp, double accuracy) {
        validateSessionIsActive();
        validateGPSData(location, speed, bearing, gpsTimestamp, accuracy);

        gpsPointsReceived++;

        if (!isGPSDataAccurate(accuracy, gpsTimestamp)) {
            filteredGpsPointsCount++;
            return;
        }

        updateLocationData(location, speed, bearing, gpsTimestamp);
        updateSessionMetrics(speed);
        storeGPSPoint(location, speed, bearing, gpsTimestamp, accuracy);

        validGpsPointsCount++;
        markAsModified();

        addDomainEvent(GPSDataReceivedEvent.of(
                getId(), vehicleId, location, speed, bearing,
                currentLocation, calculateDistanceFromPrevious(location)
        ));
    }

    public void endSession(String endedBy, String reason) {
        validateCanEnd();

        this.status = SessionStatus.ENDED;
        this.endTime = Timestamp.now();

        calculateFinalMetrics();
        markAsModified();

        addDomainEvent(TrackingSessionEndedEvent.of(
                getId(), vehicleId, routeId, getSessionDuration(),
                totalDistanceTraveled, Speed.ofKmh(averageSpeed), endedBy, reason
        ));
    }

    public void suspendSession(String reason) {
        if (status != SessionStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "Can only suspend active sessions",
                    "INVALID_SESSION_STATE_FOR_SUSPEND"
            );
        }

        this.status = SessionStatus.SUSPENDED;
        markAsModified();
    }

    public void resumeSession() {
        if (status != SessionStatus.SUSPENDED) {
            throw new BusinessRuleViolationException(
                    "Can only resume suspended sessions",
                    "INVALID_SESSION_STATE_FOR_RESUME"
            );
        }

        this.status = SessionStatus.ACTIVE;
        markAsModified();
    }

    private void validateCanStart() {
        if (status != SessionStatus.CREATED) {
            throw new BusinessRuleViolationException(
                    "Session can only be started from CREATED state",
                    "INVALID_SESSION_STATE_FOR_START"
            );
        }
    }

    private void validateCanEnd() {
        if (status == SessionStatus.ENDED) {
            throw new BusinessRuleViolationException(
                    "Session is already ended",
                    "SESSION_ALREADY_ENDED"
            );
        }

        if (startTime == null) {
            throw new BusinessRuleViolationException(
                    "Cannot end session that was never started",
                    "SESSION_NEVER_STARTED"
            );
        }
    }

    private void validateSessionIsActive() {
        if (status != SessionStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    String.format("Cannot process GPS data for session in %s state", status),
                    "INVALID_SESSION_STATE_FOR_GPS"
            );
        }

        if (hasExceededMaxDuration()) {
            throw new BusinessRuleViolationException(
                    "Session has exceeded maximum duration of " + MAX_SESSION_DURATION_HOURS + " hours",
                    "SESSION_DURATION_EXCEEDED"
            );
        }
    }

    private void validateGPSData(GeoCoordinate location, Speed speed, Bearing bearing,
                                 Timestamp gpsTimestamp, double accuracy) {
        if (location == null) {
            throw new BusinessRuleViolationException(
                    "GPS location cannot be null",
                    "INVALID_GPS_LOCATION"
            );
        }

        if (gpsTimestamp == null) {
            throw new BusinessRuleViolationException(
                    "GPS timestamp cannot be null",
                    "INVALID_GPS_TIMESTAMP"
            );
        }

        if (speed == null || bearing == null) {
            throw new BusinessRuleViolationException(
                    "GPS speed and bearing cannot be null",
                    "INVALID_GPS_SPEED_BEARING"
            );
        }

        // GPS timestamp should not be too old
        if (isGPSDataStale(gpsTimestamp)) {
            throw new BusinessRuleViolationException(
                    "GPS data is too old: " + gpsTimestamp,
                    "STALE_GPS_DATA"
            );
        }

        if (lastGpsUpdate != null && gpsTimestamp.isBefore(lastGpsUpdate)) {
            throw new BusinessRuleViolationException(
                    "GPS timestamp must be sequential",
                    "NON_SEQUENTIAL_GPS_TIMESTAMP"
            );
        }
    }

    private boolean isGPSDataAccurate(double accuracy, Timestamp gpsTimestamp) {
        return accuracy > 0 && accuracy <= MIN_GPS_ACCURACY_METERS;
    }

    private boolean isGPSDataStale(Timestamp gpsTimestamp) {
        Timestamp cutoff = Timestamp.now().minusMillis(MAX_GPS_DATA_AGE_MINUTES * 60 * 1000);
        return gpsTimestamp.isBefore(cutoff);
    }

    private boolean hasExceededMaxDuration() {
        if (startTime == null) return false;

        long durationHours = getSessionDuration().toHours();
        return durationHours > MAX_SESSION_DURATION_HOURS;
    }

    private void updateLocationData(GeoCoordinate location, Speed speed, Bearing bearing,
                                    Timestamp gpsTimestamp) {
        if (currentLocation != null) {
            double distance = currentLocation.distanceTo(location);
            if (distance > 0) { // Filter out GPS noise (same location)
                totalDistanceTraveled += distance;
            }
        }

        this.currentLocation = location;
        this.currentSpeed = speed;
        this.currentBearing = bearing;
        this.lastGpsUpdate = gpsTimestamp;
    }

    private void updateSessionMetrics(Speed speed) {

        if (maxSpeed == null || speed.isGreaterThan(maxSpeed)) {
            maxSpeed = speed;
        }

        // Track stopped time
        if (speed.isStationary()) {
            // This would need more sophisticated logic to track continuous stopped periods
            // For now, we approximate based on GPS update frequency
        }

        // Update GPS accuracy percentage
        if (gpsPointsReceived > 0) {
            gpsAccuracyPercentage = (double) validGpsPointsCount / gpsPointsReceived * 100.0;
        }
    }

    private void storeGPSPoint(GeoCoordinate location, Speed speed, Bearing bearing,
                               Timestamp timestamp, double accuracy) {
        GPSDataPoint point = new GPSDataPoint(location, speed, bearing, timestamp, accuracy);

        recentGpsPoints.add(point);

        while (recentGpsPoints.size() > MAX_GPS_POINTS_IN_MEMORY) {
            recentGpsPoints.removeFirst();
        }
    }

    private double calculateDistanceFromPrevious(GeoCoordinate newLocation) {
        if (currentLocation == null) return 0.0;
        return currentLocation.distanceTo(newLocation);
    }

    private void calculateFinalMetrics() {
        if (startTime != null && endTime != null && totalDistanceTraveled > 0) {
            long durationMinutes = java.time.Duration.between(
                    startTime.toInstant(), endTime.toInstant()
            ).toMinutes();

            if (durationMinutes > 0) {
                double distanceKm = totalDistanceTraveled / 1000.0;
                double durationHours = durationMinutes / 60.0;
                averageSpeed = distanceKm / durationHours;
            }
        }
    }

    private VehicleId validateVehicleId(VehicleId vehicleId) {
        if (vehicleId == null) {
            throw new BusinessRuleViolationException(
                    "Vehicle ID cannot be null",
                    "INVALID_VEHICLE_ID"
            );
        }
        return vehicleId;
    }

    private String validateRouteId(String routeId) {
        if (routeId == null || routeId.trim().isEmpty()) {
            throw new BusinessRuleViolationException(
                    "Route ID cannot be null or empty",
                    "INVALID_ROUTE_ID"
            );
        }
        return routeId.trim();
    }


    public List<GPSDataPoint> getRecentGpsPoints() {
        return Collections.unmodifiableList(recentGpsPoints);
    }

    public Duration getSessionDuration() {
        if (startTime == null) {
            return java.time.Duration.ZERO;
        }

        Timestamp endTimestamp = endTime != null ? endTime : Timestamp.now();
        return java.time.Duration.between(startTime.toInstant(), endTimestamp.toInstant());
    }

    public boolean isActive() {
        return status == SessionStatus.ACTIVE;
    }

    public boolean hasEnded() {
        return status == SessionStatus.ENDED;
    }

    public boolean hasRecentGPSData() {
        if (lastGpsUpdate == null) return false;

        long minutesSinceUpdate = java.time.Duration.between(
                lastGpsUpdate.toInstant(),
                Timestamp.now().toInstant()
        ).toMinutes();

        return minutesSinceUpdate <= 5; // Consider data fresh if within 5 minutes
    }

    public SessionQuality getSessionQuality() {
        if (gpsPointsReceived == 0) {
            return SessionQuality.NO_DATA;
        }

        double accuracyScore = gpsAccuracyPercentage;
        double dataCompletenessScore = (double) validGpsPointsCount / gpsPointsReceived * 100;
        double overallScore = (accuracyScore + dataCompletenessScore) / 2.0;

        if (overallScore >= 90) {
            return SessionQuality.EXCELLENT;
        } else if (overallScore >= 75) {
            return SessionQuality.GOOD;
        } else if (overallScore >= 60) {
            return SessionQuality.FAIR;
        } else {
            return SessionQuality.POOR;
        }
    }

    @Getter
    public enum SessionStatus {
        CREATED("Created"),
        ACTIVE("Active"),
        SUSPENDED("Suspended"),
        ENDED("Ended");

        private final String description;

        SessionStatus(String description) {
            this.description = description;
        }

    }

    @Getter
    public enum SessionQuality {
        EXCELLENT("Excellent", "green"),
        GOOD("Good", "lightgreen"),
        FAIR("Fair", "yellow"),
        POOR("Poor", "red"),
        NO_DATA("No Data", "gray");

        private final String description;
        private final String colorCode;

        SessionQuality(String description, String colorCode) {
            this.description = description;
            this.colorCode = colorCode;
        }

    }

    public static record GPSDataPoint(
            GeoCoordinate location,
            Speed speed,
            Bearing bearing,
            Timestamp timestamp,
            double accuracy
    ) {
        public GPSDataPoint {
            if (location == null || timestamp == null) {
                throw new IllegalArgumentException("Location and timestamp cannot be null");
            }
            if (accuracy < 0) {
                throw new IllegalArgumentException("Accuracy cannot be negative");
            }
        }

        public boolean isNavigationQuality() {
            return accuracy <= 20.0;
        }

        public boolean indicatesMovement() {
            return speed != null && speed.isMoving();
        }

        public java.time.Duration getAge() {
            return java.time.Duration.between(timestamp.toInstant(), Timestamp.now().toInstant());
        }
    }
}