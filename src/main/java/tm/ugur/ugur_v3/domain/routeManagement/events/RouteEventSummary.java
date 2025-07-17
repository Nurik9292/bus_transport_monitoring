package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public final class RouteEventSummary {

    private final RouteId routeId;
    private final Timestamp periodStart;
    private final Timestamp periodEnd;
    private final List<BaseRouteEvent> events;
    private final Map<String, Long> eventTypeCounts;
    private final int totalEvents;
    private final boolean hasAlerts;
    private final boolean hasPerformanceIssues;

    public RouteEventSummary(RouteId routeId, Timestamp periodStart, Timestamp periodEnd,
                             List<BaseRouteEvent> events) {
        this.routeId = routeId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.events = List.copyOf(events);
        this.eventTypeCounts = calculateEventTypeCounts();
        this.totalEvents = events.size();
        this.hasAlerts = events.stream().anyMatch(this::isAlertEvent);
        this.hasPerformanceIssues = events.stream().anyMatch(this::isPerformanceEvent);
    }

    private Map<String, Long> calculateEventTypeCounts() {
        return events.stream()
                .collect(Collectors.groupingBy(
                        BaseRouteEvent::getEventType,
                        Collectors.counting()
                ));
    }

    private boolean isAlertEvent(BaseRouteEvent event) {
        return event instanceof RouteMaintenanceAlertEvent ||
                event instanceof RouteServiceInterruptedEvent ||
                event instanceof RouteCapacityExceededEvent;
    }

    private boolean isPerformanceEvent(BaseRouteEvent event) {
        return event instanceof RoutePerformanceUpdatedEvent ||
                event instanceof RouteMaintenanceAlertEvent;
    }

    public List<BaseRouteEvent> getAlertEvents() {
        return events.stream()
                .filter(this::isAlertEvent)
                .collect(Collectors.toList());
    }

    public List<BaseRouteEvent> getPerformanceEvents() {
        return events.stream()
                .filter(this::isPerformanceEvent)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return String.format("RouteEventSummary{routeId=%s, period=%s to %s, total=%d, alerts=%s, performance=%s}",
                routeId, periodStart, periodEnd, totalEvents, hasAlerts, hasPerformanceIssues);
    }
}