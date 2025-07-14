package tm.ugur.ugur_v3.domain.vehicleManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.TrackingSessionId;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.Speed;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;


@Getter
public final class TrackingSessionEndedEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;


    private final TrackingSessionId sessionId;
    private final VehicleId vehicleId;
    private final String routeId;
    private final Duration sessionDuration;
    private final double totalDistanceTraveled;
    private final Speed averageSpeed;
    private final String endedBy;
    private final SessionEndReason reason;
    private final SessionSummary sessionSummary;

    private final Map<String, String> metadata;

    private TrackingSessionEndedEvent(TrackingSessionId sessionId, VehicleId vehicleId,
                                      String routeId, Duration sessionDuration,
                                      double totalDistanceTraveled, Speed averageSpeed,
                                      String endedBy, SessionEndReason reason,
                                      SessionSummary sessionSummary,
                                      String correlationId, Map<String, String> metadata) {

        this.eventId = UUID.randomUUID().toString();
        this.eventType = "TrackingSessionEnded";
        this.occurredAt = Timestamp.now();
        this.aggregateId = sessionId.getValue();
        this.aggregateType = "VehicleTrackingSession";
        this.version = 1L;
        this.correlationId = correlationId;

        this.sessionId = sessionId;
        this.vehicleId = vehicleId;
        this.routeId = routeId;
        this.sessionDuration = sessionDuration;
        this.totalDistanceTraveled = totalDistanceTraveled;
        this.averageSpeed = averageSpeed;
        this.endedBy = endedBy;
        this.reason = reason != null ? reason : SessionEndReason.MANUAL;
        this.sessionSummary = sessionSummary;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public static TrackingSessionEndedEvent of(TrackingSessionId sessionId, VehicleId vehicleId,
                                               String routeId, Duration sessionDuration,
                                               double totalDistanceTraveled, Speed averageSpeed,
                                               String endedBy, String reason) {
        return new TrackingSessionEndedEvent(
                sessionId, vehicleId, routeId, sessionDuration, totalDistanceTraveled,
                averageSpeed, endedBy, SessionEndReason.MANUAL, null, null, null
        );
    }

    public static TrackingSessionEndedEvent of(TrackingSessionId sessionId, VehicleId vehicleId,
                                               String routeId, Duration sessionDuration,
                                               double totalDistanceTraveled, Speed averageSpeed,
                                               String endedBy, SessionEndReason reason,
                                               SessionSummary sessionSummary) {
        return new TrackingSessionEndedEvent(
                sessionId, vehicleId, routeId, sessionDuration, totalDistanceTraveled,
                averageSpeed, endedBy, reason, sessionSummary, null, null
        );
    }

    public static TrackingSessionEndedEvent of(TrackingSessionId sessionId, VehicleId vehicleId,
                                               String routeId, Duration sessionDuration,
                                               double totalDistanceTraveled, Speed averageSpeed,
                                               String endedBy, SessionEndReason reason,
                                               SessionSummary sessionSummary,
                                               String correlationId, Map<String, String> metadata) {
        return new TrackingSessionEndedEvent(
                sessionId, vehicleId, routeId, sessionDuration, totalDistanceTraveled,
                averageSpeed, endedBy, reason, sessionSummary, correlationId, metadata
        );
    }

    public boolean isNormalEnd() {
        return reason == SessionEndReason.MANUAL ||
                reason == SessionEndReason.ROUTE_COMPLETED ||
                reason == SessionEndReason.SHIFT_ENDED ||
                reason == SessionEndReason.SCHEDULED;
    }

    public boolean isErrorEnd() {
        return reason == SessionEndReason.SYSTEM_ERROR ||
                reason == SessionEndReason.GPS_FAILURE ||
                reason == SessionEndReason.COMMUNICATION_LOST ||
                reason == SessionEndReason.VEHICLE_BREAKDOWN;
    }

    public boolean isPrematureEnd() {
        return sessionDuration.toMinutes() < 10 ||
                reason == SessionEndReason.EMERGENCY_STOP ||
                reason == SessionEndReason.VEHICLE_BREAKDOWN;
    }

    public boolean isSuccessfulSession() {
        return isNormalEnd() &&
                sessionDuration.toMinutes() >= 10 &&
                totalDistanceTraveled > 0 &&
                (sessionSummary == null || sessionSummary.gpsAccuracyPercentage() >= 80.0);
    }

    public SessionPerformance getSessionPerformance() {
        if (isErrorEnd() || isPrematureEnd()) {
            return SessionPerformance.POOR;
        }

        if (sessionSummary == null) {
            return SessionPerformance.UNKNOWN;
        }

        double accuracyScore = sessionSummary.gpsAccuracyPercentage();
        double completenessScore = sessionSummary.dataCompletenessPercentage();
        double overallScore = (accuracyScore + completenessScore) / 2.0;

        if (overallScore >= 90.0) {
            return SessionPerformance.EXCELLENT;
        } else if (overallScore >= 75.0) {
            return SessionPerformance.GOOD;
        } else if (overallScore >= 60.0) {
            return SessionPerformance.FAIR;
        } else {
            return SessionPerformance.POOR;
        }
    }

    public int getProcessingPriority() {
        if (isErrorEnd()) {
            return 1;
        } else if (isPrematureEnd()) {
            return 2;
        } else {
            return 3;
        }
    }

    public boolean shouldArchiveSessionData() {
        return isSuccessfulSession() ||
                sessionDuration.toMinutes() >= 30;
    }

    public boolean requiresInvestigation() {
        return isErrorEnd() ||
                isPrematureEnd() ||
                (sessionSummary != null && sessionSummary.gpsAccuracyPercentage() < 50.0);
    }

    public double getTotalDistanceKm() {
        return totalDistanceTraveled / 1000.0;
    }

    public double getSessionDurationHours() {
        return sessionDuration.toMinutes() / 60.0;
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getAggregateId() {
        return aggregateId;
    }

    @Override
    public Timestamp getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String getAggregateType() {
        return aggregateType;
    }

    @Override
    public Long getVersion() {
        return version;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> allMetadata = new java.util.HashMap<>(metadata);
        allMetadata.put("sessionId", sessionId.getValue());
        allMetadata.put("vehicleId", vehicleId.getValue());
        allMetadata.put("endedBy", endedBy);
        allMetadata.put("reason", reason.name());
        allMetadata.put("durationMinutes", sessionDuration.toMinutes());
        allMetadata.put("durationHours", getSessionDurationHours());
        allMetadata.put("distanceMeters", totalDistanceTraveled);
        allMetadata.put("distanceKm", getTotalDistanceKm());
        allMetadata.put("isNormalEnd", isNormalEnd());
        allMetadata.put("isErrorEnd", isErrorEnd());
        allMetadata.put("isPrematureEnd", isPrematureEnd());
        allMetadata.put("isSuccessful", isSuccessfulSession());
        allMetadata.put("performance", getSessionPerformance().name());
        allMetadata.put("processingPriority", getProcessingPriority());
        allMetadata.put("shouldArchive", shouldArchiveSessionData());
        allMetadata.put("requiresInvestigation", requiresInvestigation());

        if (routeId != null) {
            allMetadata.put("routeId", routeId);
        }
        if (averageSpeed != null) {
            allMetadata.put("averageSpeedKmh", averageSpeed.getKmh());
        }
        if (sessionSummary != null) {
            allMetadata.put("gpsPointsReceived", sessionSummary.totalGpsPoints());
            allMetadata.put("validGpsPoints", sessionSummary.validGpsPoints());
            allMetadata.put("gpsAccuracyPct", sessionSummary.gpsAccuracyPercentage());
            allMetadata.put("dataCompletenessPct", sessionSummary.dataCompletenessPercentage());
        }

        return Map.copyOf(allMetadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        TrackingSessionEndedEvent that = (TrackingSessionEndedEvent) obj;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("TrackingSessionEndedEvent{eventId='%s', sessionId=%s, vehicleId=%s, duration=%s, distance=%.1fkm, reason=%s, performance=%s, timestamp=%s}",
                eventId.substring(0, 8) + "...",
                sessionId.getShortFormat(),
                vehicleId,
                formatDuration(sessionDuration),
                getTotalDistanceKm(),
                reason,
                getSessionPerformance(),
                occurredAt
        );
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        return String.format("%dh%02dm", hours, minutes);
    }

    @Getter
    public enum SessionEndReason {
        MANUAL("Manual End"),
        ROUTE_COMPLETED("Route Completed"),
        SHIFT_ENDED("Shift Ended"),
        SCHEDULED("Scheduled End"),
        EMERGENCY_STOP("Emergency Stop"),
        VEHICLE_BREAKDOWN("Vehicle Breakdown"),
        SYSTEM_ERROR("System Error"),
        GPS_FAILURE("GPS Failure"),
        COMMUNICATION_LOST("Communication Lost"),
        TIMEOUT("Session Timeout");

        private final String description;

        SessionEndReason(String description) {
            this.description = description;
        }

    }

    @Getter
    public enum SessionPerformance {
        EXCELLENT("Excellent", "green"),
        GOOD("Good", "lightgreen"),
        FAIR("Fair", "yellow"),
        POOR("Poor", "red"),
        UNKNOWN("Unknown", "gray");

        private final String description;
        private final String colorCode;

        SessionPerformance(String description, String colorCode) {
            this.description = description;
            this.colorCode = colorCode;
        }

    }

    public record SessionSummary(
            int totalGpsPoints,
            int validGpsPoints,
            int filteredGpsPoints,
            double gpsAccuracyPercentage,
            double dataCompletenessPercentage,
            Speed maxSpeed,
            long stoppedTimeMinutes,
            int significantStops
    ) {
        public SessionSummary {
            if (totalGpsPoints < 0 || validGpsPoints < 0 || filteredGpsPoints < 0) {
                throw new IllegalArgumentException("GPS point counts cannot be negative");
            }
            if (gpsAccuracyPercentage < 0 || gpsAccuracyPercentage > 100) {
                throw new IllegalArgumentException("GPS accuracy percentage must be between 0 and 100");
            }
            if (dataCompletenessPercentage < 0 || dataCompletenessPercentage > 100) {
                throw new IllegalArgumentException("Data completeness percentage must be between 0 and 100");
            }
        }

        public double getDataQualityScore() {
            return (gpsAccuracyPercentage + dataCompletenessPercentage) / 2.0;
        }

        public boolean hasGoodDataQuality() {
            return getDataQualityScore() >= 80.0;
        }
    }
}