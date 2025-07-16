package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

@Getter
public final class RoutePerformanceUpdatedEvent implements DomainEvent {


    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;


    private final RouteId routeId;
    private final double averageSpeed;
    private final int dailyRidership;
    private final double onTimePerformance;
    private final double previousOnTimePerformance;
    private final boolean performanceImproved;
    private final boolean alertThresholdExceeded;
    private final Map<String, Object> metadata;

    private RoutePerformanceUpdatedEvent(RouteId routeId, double averageSpeed, int dailyRidership,
                                         double onTimePerformance, double previousOnTimePerformance,
                                         String correlationId, Map<String, Object> metadata) {


        this.eventId = UUID.randomUUID().toString();
        this.eventType = "RoutePerformanceUpdated";
        this.occurredAt = Timestamp.now();
        this.aggregateId = routeId.getValue();
        this.aggregateType = "Route";
        this.version = 1L;
        this.correlationId = correlationId;


        this.routeId = validateRouteId(routeId);
        this.averageSpeed = validateAverageSpeed(averageSpeed);
        this.dailyRidership = validateDailyRidership(dailyRidership);
        this.onTimePerformance = validateOnTimePerformance(onTimePerformance);
        this.previousOnTimePerformance = validateOnTimePerformance(previousOnTimePerformance);


        this.performanceImproved = onTimePerformance > previousOnTimePerformance;
        this.alertThresholdExceeded = onTimePerformance < 70.0;

        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public static RoutePerformanceUpdatedEvent of(RouteId routeId, double averageSpeed,
                                                  int dailyRidership, double onTimePerformance) {
        return new RoutePerformanceUpdatedEvent(routeId, averageSpeed, dailyRidership,
                onTimePerformance, onTimePerformance, null, null);
    }

    public static RoutePerformanceUpdatedEvent of(RouteId routeId, double averageSpeed,
                                                  int dailyRidership, double onTimePerformance,
                                                  double previousOnTimePerformance) {
        return new RoutePerformanceUpdatedEvent(routeId, averageSpeed, dailyRidership,
                onTimePerformance, previousOnTimePerformance, null, null);
    }

    public static RoutePerformanceUpdatedEvent of(RouteId routeId, double averageSpeed,
                                                  int dailyRidership, double onTimePerformance,
                                                  double previousOnTimePerformance, String correlationId,
                                                  Map<String, Object> metadata) {
        return new RoutePerformanceUpdatedEvent(routeId, averageSpeed, dailyRidership,
                onTimePerformance, previousOnTimePerformance,
                correlationId, metadata);
    }



    public boolean isSignificantImprovement() {
        return performanceImproved && (onTimePerformance - previousOnTimePerformance) >= 5.0;
    }

    public boolean isSignificantDeterioration() {
        return !performanceImproved && (previousOnTimePerformance - onTimePerformance) >= 5.0;
    }

    public boolean isCriticalPerformance() {
        return onTimePerformance < 50.0;
    }

    public boolean isGoodPerformance() {
        return onTimePerformance >= 90.0;
    }

    public PerformanceLevel getPerformanceLevel() {
        if (onTimePerformance >= 95.0) return PerformanceLevel.EXCELLENT;
        if (onTimePerformance >= 90.0) return PerformanceLevel.GOOD;
        if (onTimePerformance >= 80.0) return PerformanceLevel.ACCEPTABLE;
        if (onTimePerformance >= 70.0) return PerformanceLevel.POOR;
        return PerformanceLevel.CRITICAL;
    }

    public double getPerformanceChange() {
        if (previousOnTimePerformance == 0) return 0.0;
        return onTimePerformance - previousOnTimePerformance;
    }

    public boolean requiresImmediateAttention() {
        return isCriticalPerformance() ||
                isSignificantDeterioration() ||
                (averageSpeed < 10.0) ||
                (dailyRidership == 0);
    }



    private RouteId validateRouteId(RouteId routeId) {
        if (routeId == null) {
            throw new IllegalArgumentException("RouteId cannot be null");
        }
        return routeId;
    }

    private double validateAverageSpeed(double speed) {
        if (speed < 0 || speed > 200) {
            throw new IllegalArgumentException("Average speed must be between 0 and 200 km/h, got: " + speed);
        }
        return speed;
    }

    private int validateDailyRidership(int ridership) {
        if (ridership < 0) {
            throw new IllegalArgumentException("Daily ridership cannot be negative, got: " + ridership);
        }
        return ridership;
    }

    private double validateOnTimePerformance(double performance) {
        if (performance < 0 || performance > 100) {
            throw new IllegalArgumentException("On-time performance must be between 0 and 100%, got: " + performance);
        }
        return performance;
    }



    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> enhancedMetadata = new HashMap<>(metadata);


        enhancedMetadata.put("performanceLevel", getPerformanceLevel().name());
        enhancedMetadata.put("performanceChange", getPerformanceChange());
        enhancedMetadata.put("isSignificantImprovement", isSignificantImprovement());
        enhancedMetadata.put("isSignificantDeterioration", isSignificantDeterioration());
        enhancedMetadata.put("requiresImmediateAttention", requiresImmediateAttention());
        enhancedMetadata.put("isCriticalPerformance", isCriticalPerformance());
        enhancedMetadata.put("isGoodPerformance", isGoodPerformance());


        if (averageSpeed > 50) {
            enhancedMetadata.put("speedCategory", "HIGH");
        } else if (averageSpeed > 25) {
            enhancedMetadata.put("speedCategory", "NORMAL");
        } else if (averageSpeed > 10) {
            enhancedMetadata.put("speedCategory", "LOW");
        } else {
            enhancedMetadata.put("speedCategory", "CRITICAL");
        }


        if (dailyRidership > 1000) {
            enhancedMetadata.put("ridershipCategory", "HIGH");
        } else if (dailyRidership > 500) {
            enhancedMetadata.put("ridershipCategory", "MEDIUM");
        } else if (dailyRidership > 100) {
            enhancedMetadata.put("ridershipCategory", "LOW");
        } else {
            enhancedMetadata.put("ridershipCategory", "MINIMAL");
        }

        return Map.copyOf(enhancedMetadata);
    }


    @Getter
    public enum PerformanceLevel {
        EXCELLENT("Excellent", "green", 95.0),
        GOOD("Good", "lightgreen", 90.0),
        ACCEPTABLE("Acceptable", "yellow", 80.0),
        POOR("Poor", "orange", 70.0),
        CRITICAL("Critical", "red", 0.0);

        private final String displayName;
        private final String colorCode;
        private final double threshold;

        PerformanceLevel(String displayName, String colorCode, double threshold) {
            this.displayName = displayName;
            this.colorCode = colorCode;
            this.threshold = threshold;
        }

        public boolean isAboveThreshold(double performance) {
            return performance >= threshold;
        }
    }


    @Override
    public String toString() {
        return String.format("RoutePerformanceUpdatedEvent{routeId=%s, onTime=%.1f%%, speed=%.1f km/h, ridership=%d, level=%s, improved=%s}",
                routeId, onTimePerformance, averageSpeed, dailyRidership,
                getPerformanceLevel(), performanceImproved);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        RoutePerformanceUpdatedEvent that = (RoutePerformanceUpdatedEvent) obj;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }
}