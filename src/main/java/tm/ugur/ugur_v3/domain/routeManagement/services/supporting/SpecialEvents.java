package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.util.List;

public record SpecialEvents(
        List<SpecialEvent> events,
        Timestamp validFrom,
        Timestamp validTo
) {

    public record SpecialEvent(
            String eventId,
            String eventName,
            EventType type,
            GeoCoordinate location,
            double radiusKm,
            Timestamp startTime,
            Timestamp endTime,
            int expectedAttendees,
            EventImpact impact
    ) {

        public enum EventType {
            CONCERT, FESTIVAL, SPORTS, CONFERENCE, PARADE, DEMONSTRATION, CONSTRUCTION, HOLIDAY
        }

        @Getter
        public enum EventImpact {
            LOW(1.1),
            MEDIUM(1.3),
            HIGH(1.6),
            CRITICAL(2.0);

            private final double demandMultiplier;

            EventImpact(double demandMultiplier) {
                this.demandMultiplier = demandMultiplier;
            }

        }

        public boolean isActive(Timestamp timestamp) {
            return !timestamp.isBefore(startTime) && !timestamp.isAfter(endTime);
        }

        public boolean affectsLocation(GeoCoordinate checkLocation, double checkRadiusKm) {
            // Simplified distance check - в реальности нужен proper geographic calculation
            return true; // Stub implementation
        }
    }

    public static SpecialEvents empty() {
        return new SpecialEvents(
                List.of(),
                Timestamp.now(),
                Timestamp.now().plusDays(7)
        );
    }

    public List<SpecialEvent> getActiveEvents(Timestamp timestamp) {
        return events.stream()
                .filter(event -> event.isActive(timestamp))
                .toList();
    }

    public boolean hasHighImpactEvents(Timestamp timestamp) {
        return getActiveEvents(timestamp).stream()
                .anyMatch(event -> event.impact() == SpecialEvent.EventImpact.HIGH ||
                        event.impact() == SpecialEvent.EventImpact.CRITICAL);
    }

    public double getTotalDemandMultiplier(Timestamp timestamp, GeoCoordinate location) {
        return getActiveEvents(timestamp).stream()
                .filter(event -> event.affectsLocation(location, 5.0)) // 5km radius
                .mapToDouble(event -> event.impact().getDemandMultiplier())
                .max()
                .orElse(1.0);
    }
}