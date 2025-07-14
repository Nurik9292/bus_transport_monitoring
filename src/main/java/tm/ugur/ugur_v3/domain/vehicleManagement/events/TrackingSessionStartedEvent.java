package tm.ugur.ugur_v3.domain.vehicleManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.TrackingSessionId;

import java.util.Map;
import java.util.UUID;


@Getter
public final class TrackingSessionStartedEvent implements DomainEvent {

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
    private final GeoCoordinate initialLocation;
    private final String startedBy;
    private final SessionStartReason reason;
    private final Map<String, Object> sessionConfig;

    private final Map<String, String> metadata;

    private TrackingSessionStartedEvent(TrackingSessionId sessionId, VehicleId vehicleId,
                                        String routeId, GeoCoordinate initialLocation,
                                        String startedBy, SessionStartReason reason,
                                        Map<String, Object> sessionConfig,
                                        String correlationId, Map<String, String> metadata) {

        this.eventId = UUID.randomUUID().toString();
        this.eventType = "TrackingSessionStarted";
        this.occurredAt = Timestamp.now();
        this.aggregateId = sessionId.getValue();
        this.aggregateType = "VehicleTrackingSession";
        this.version = 1L;
        this.correlationId = correlationId;

        this.sessionId = sessionId;
        this.vehicleId = vehicleId;
        this.routeId = routeId;
        this.initialLocation = initialLocation;
        this.startedBy = startedBy;
        this.reason = reason != null ? reason : SessionStartReason.MANUAL;
        this.sessionConfig = sessionConfig != null ? Map.copyOf(sessionConfig) : Map.of();
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public static TrackingSessionStartedEvent of(TrackingSessionId sessionId, VehicleId vehicleId,
                                                 String routeId, GeoCoordinate initialLocation,
                                                 String startedBy) {
        return new TrackingSessionStartedEvent(
                sessionId, vehicleId, routeId, initialLocation, startedBy,
                SessionStartReason.MANUAL, null, null, null
        );
    }

    public static TrackingSessionStartedEvent of(TrackingSessionId sessionId, VehicleId vehicleId,
                                                 String routeId, GeoCoordinate initialLocation,
                                                 String startedBy, SessionStartReason reason) {
        return new TrackingSessionStartedEvent(
                sessionId, vehicleId, routeId, initialLocation, startedBy,
                reason, null, null, null
        );
    }

    public static TrackingSessionStartedEvent of(TrackingSessionId sessionId, VehicleId vehicleId,
                                                 String routeId, GeoCoordinate initialLocation,
                                                 String startedBy, SessionStartReason reason,
                                                 Map<String, Object> sessionConfig,
                                                 String correlationId, Map<String, String> metadata) {
        return new TrackingSessionStartedEvent(
                sessionId, vehicleId, routeId, initialLocation, startedBy,
                reason, sessionConfig, correlationId, metadata
        );
    }

    public boolean isAutomatic() {
        return reason == SessionStartReason.ROUTE_ASSIGNMENT ||
                reason == SessionStartReason.SCHEDULED ||
                reason == SessionStartReason.SYSTEM_RECOVERY;
    }

    public boolean requiresImmediateMonitoring() {
        return reason == SessionStartReason.EMERGENCY ||
                reason == SessionStartReason.RECOVERY_AFTER_BREAKDOWN ||
                sessionConfig.containsKey("highPriority");
    }

    public boolean isPublicTransportSession() {
        return routeId != null && !routeId.trim().isEmpty();
    }

    public int getRecommendedUpdateFrequencySeconds() {
        if (sessionConfig.containsKey("updateFrequency")) {
            return (Integer) sessionConfig.get("updateFrequency");
        }

        return switch (reason) {
            case EMERGENCY, RECOVERY_AFTER_BREAKDOWN -> 5;
            case ROUTE_ASSIGNMENT, SCHEDULED -> 15;
            case MANUAL, MAINTENANCE_TEST -> 30;
            case SYSTEM_RECOVERY -> 10;
        };
    }

    public int getProcessingPriority() {
        return switch (reason) {
            case EMERGENCY -> 0;
            case RECOVERY_AFTER_BREAKDOWN -> 1;
            case ROUTE_ASSIGNMENT -> 2;
            case SCHEDULED, SYSTEM_RECOVERY -> 3;
            case MANUAL, MAINTENANCE_TEST -> 4;
        };
    }

    public boolean shouldEnableRealTimeBroadcasting() {
        return isPublicTransportSession() &&
                reason != SessionStartReason.MAINTENANCE_TEST;
    }

    public boolean shouldStartAnalyticsCollection() {
        return reason != SessionStartReason.MAINTENANCE_TEST;
    }

    public long getExpectedDurationMinutes() {
        if (sessionConfig.containsKey("expectedDurationMinutes")) {
            return (Long) sessionConfig.get("expectedDurationMinutes");
        }

        return switch (reason) {
            case ROUTE_ASSIGNMENT, SCHEDULED -> 480;
            case EMERGENCY -> 60;
            case RECOVERY_AFTER_BREAKDOWN -> 120;
            case MAINTENANCE_TEST -> 30;
            case MANUAL -> 240;
            case SYSTEM_RECOVERY -> 60;
        };
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
        allMetadata.put("startedBy", startedBy);
        allMetadata.put("reason", reason.name());
        allMetadata.put("isAutomatic", isAutomatic());
        allMetadata.put("requiresImmediateMonitoring", requiresImmediateMonitoring());
        allMetadata.put("isPublicTransport", isPublicTransportSession());
        allMetadata.put("updateFrequencySeconds", getRecommendedUpdateFrequencySeconds());
        allMetadata.put("processingPriority", getProcessingPriority());
        allMetadata.put("shouldEnableBroadcasting", shouldEnableRealTimeBroadcasting());
        allMetadata.put("shouldStartAnalytics", shouldStartAnalyticsCollection());
        allMetadata.put("expectedDurationMinutes", getExpectedDurationMinutes());

        if (routeId != null) {
            allMetadata.put("routeId", routeId);
        }
        if (initialLocation != null) {
            allMetadata.put("initialLatitude", initialLocation.getLatitude());
            allMetadata.put("initialLongitude", initialLocation.getLongitude());
        }

        allMetadata.putAll(sessionConfig);

        return Map.copyOf(allMetadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        TrackingSessionStartedEvent that = (TrackingSessionStartedEvent) obj;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("TrackingSessionStartedEvent{eventId='%s', sessionId=%s, vehicleId=%s, route='%s', reason=%s, by='%s', timestamp=%s}",
                eventId.substring(0, 8) + "...",
                sessionId.getShortFormat(),
                vehicleId,
                routeId != null ? routeId : "none",
                reason,
                startedBy,
                occurredAt
        );
    }

    @Getter
    public enum SessionStartReason {
        MANUAL("Manual Start"),
        ROUTE_ASSIGNMENT("Route Assignment"),
        SCHEDULED("Scheduled Start"),
        EMERGENCY("Emergency Situation"),
        RECOVERY_AFTER_BREAKDOWN("Recovery After Breakdown"),
        MAINTENANCE_TEST("Maintenance Test"),
        SYSTEM_RECOVERY("System Recovery");

        private final String description;

        SessionStartReason(String description) {
            this.description = description;
        }

    }
}