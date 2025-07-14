package tm.ugur.ugur_v3.domain.vehicleManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.events.DomainEvent;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;

import java.util.Map;
import java.util.UUID;

@Getter
public final class VehicleBreakdownReportedEvent implements DomainEvent {

    private final String eventId;
    private final String eventType;
    private final Timestamp occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final String correlationId;


    private final VehicleId vehicleId;
    private final String routeId;
    private final String routeName;
    private final GeoCoordinate breakdownLocation;
    private final BreakdownSeverity severity;
    private final BreakdownType breakdownType;
    private final String description;
    private final String reportedBy;
    private final boolean passengersOnBoard;
    private final Integer estimatedPassengerCount;
    private final boolean requiresEmergencyServices;
    private final boolean blocksTraffic;
    private final VehicleStatus previousStatus;
    private final String nearestLandmark;
    private final Double temperatureC;

    private final Map<String, String> metadata;

    private VehicleBreakdownReportedEvent(VehicleId vehicleId, String routeId, String routeName,
                                          GeoCoordinate breakdownLocation, BreakdownSeverity severity,
                                          BreakdownType breakdownType, String description,
                                          String reportedBy, boolean passengersOnBoard,
                                          Integer estimatedPassengerCount, boolean requiresEmergencyServices,
                                          boolean blocksTraffic, VehicleStatus previousStatus,
                                          String nearestLandmark, Double temperatureC,
                                          String correlationId, Map<String, String> metadata) {

        this.eventId = UUID.randomUUID().toString();
        this.eventType = "VehicleBreakdownReported";
        this.occurredAt = Timestamp.now();
        this.aggregateId = vehicleId.getValue();
        this.aggregateType = "Vehicle";
        this.version = 1L;
        this.correlationId = correlationId;

        this.vehicleId = vehicleId;
        this.routeId = routeId;
        this.routeName = routeName;
        this.breakdownLocation = breakdownLocation;
        this.severity = severity;
        this.breakdownType = breakdownType;
        this.description = description;
        this.reportedBy = reportedBy;
        this.passengersOnBoard = passengersOnBoard;
        this.estimatedPassengerCount = estimatedPassengerCount;
        this.requiresEmergencyServices = requiresEmergencyServices;
        this.blocksTraffic = blocksTraffic;
        this.previousStatus = previousStatus;
        this.nearestLandmark = nearestLandmark;
        this.temperatureC = temperatureC;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public static VehicleBreakdownReportedEvent of(VehicleId vehicleId, String routeId,
                                                   GeoCoordinate breakdownLocation,
                                                   BreakdownSeverity severity, String description) {
        return new VehicleBreakdownReportedEvent(
                vehicleId, routeId, null, breakdownLocation, severity,
                BreakdownType.UNKNOWN, description, "SYSTEM", false,
                null, false, false, VehicleStatus.ACTIVE, null, null, null, null
        );
    }

    public static VehicleBreakdownReportedEvent of(VehicleId vehicleId, String routeId, String routeName,
                                                   GeoCoordinate breakdownLocation, BreakdownSeverity severity,
                                                   BreakdownType breakdownType, String description,
                                                   String reportedBy, boolean passengersOnBoard,
                                                   Integer estimatedPassengerCount,
                                                   boolean requiresEmergencyServices,
                                                   boolean blocksTraffic, String nearestLandmark) {
        return new VehicleBreakdownReportedEvent(
                vehicleId, routeId, routeName, breakdownLocation, severity, breakdownType,
                description, reportedBy, passengersOnBoard, estimatedPassengerCount,
                requiresEmergencyServices, blocksTraffic, VehicleStatus.IN_ROUTE,
                nearestLandmark, null, null, null
        );
    }

    public static VehicleBreakdownReportedEvent of(VehicleId vehicleId, String routeId, String routeName,
                                                   GeoCoordinate breakdownLocation, BreakdownSeverity severity,
                                                   BreakdownType breakdownType, String description,
                                                   String reportedBy, boolean passengersOnBoard,
                                                   Integer estimatedPassengerCount,
                                                   boolean requiresEmergencyServices, boolean blocksTraffic,
                                                   VehicleStatus previousStatus, String nearestLandmark,
                                                   Double temperatureC, String correlationId,
                                                   Map<String, String> metadata) {
        return new VehicleBreakdownReportedEvent(
                vehicleId, routeId, routeName, breakdownLocation, severity, breakdownType,
                description, reportedBy, passengersOnBoard, estimatedPassengerCount,
                requiresEmergencyServices, blocksTraffic, previousStatus, nearestLandmark,
                temperatureC, correlationId, metadata
        );
    }

    public boolean isCriticalEmergency() {
        return severity == BreakdownSeverity.CRITICAL ||
                requiresEmergencyServices ||
                (passengersOnBoard && severity == BreakdownSeverity.HIGH);
    }

    public boolean requiresPassengerEvacuation() {
        return passengersOnBoard &&
                (severity == BreakdownSeverity.CRITICAL ||
                        breakdownType == BreakdownType.FIRE ||
                        breakdownType == BreakdownType.ELECTRICAL_FAULT ||
                        requiresEmergencyServices);
    }

    public boolean shouldArrangeAlternativeTransport() {
        return passengersOnBoard &&
                estimatedPassengerCount != null &&
                estimatedPassengerCount > 0 &&
                severity != BreakdownSeverity.LOW;
    }

    public boolean requiresTrafficControl() {
        return blocksTraffic ||
                (severity == BreakdownSeverity.CRITICAL && breakdownLocation != null);
    }

    public boolean requiresImmediateMaintenance() {
        return severity == BreakdownSeverity.HIGH ||
                severity == BreakdownSeverity.CRITICAL ||
                breakdownType == BreakdownType.ENGINE_FAILURE ||
                breakdownType == BreakdownType.ELECTRICAL_FAULT;
    }

    public ResponseUrgency getResponseUrgency() {
        if (isCriticalEmergency()) {
            return ResponseUrgency.IMMEDIATE;
        } else if (severity == BreakdownSeverity.HIGH || passengersOnBoard) {
            return ResponseUrgency.URGENT;
        } else if (severity == BreakdownSeverity.MEDIUM) {
            return ResponseUrgency.PRIORITY;
        } else {
            return ResponseUrgency.NORMAL;
        }
    }

    public int getProcessingPriority() {
        return switch (getResponseUrgency()) {
            case IMMEDIATE -> 0;
            case URGENT -> 1;
            case PRIORITY -> 2;
            case NORMAL -> 3;
        };
    }

    public boolean shouldNotifyEmergencyServices() {
        return requiresEmergencyServices ||
                breakdownType == BreakdownType.FIRE ||
                breakdownType == BreakdownType.ACCIDENT ||
                isCriticalEmergency();
    }

    public boolean shouldSuspendRoute() {
        return severity == BreakdownSeverity.CRITICAL ||
                blocksTraffic ||
                requiresPassengerEvacuation();
    }

    public EstimatedRepairTime getEstimatedRepairTime() {
        return switch (breakdownType) {
            case ENGINE_FAILURE, TRANSMISSION_FAULT -> EstimatedRepairTime.HOURS_TO_DAYS;
            case ELECTRICAL_FAULT, BRAKE_ISSUE -> EstimatedRepairTime.HOURS;
            case TIRE_PUNCTURE, DOOR_MALFUNCTION -> EstimatedRepairTime.MINUTES_TO_HOUR;
            case MINOR_MECHANICAL -> EstimatedRepairTime.MINUTES;
            case FIRE, ACCIDENT -> EstimatedRepairTime.DAYS_TO_WEEKS;
            case UNKNOWN -> severity == BreakdownSeverity.HIGH ?
                    EstimatedRepairTime.HOURS : EstimatedRepairTime.UNKNOWN;
        };
    }

    public boolean hasAdequateInformation() {
        return breakdownLocation != null &&
                severity != null &&
                description != null && !description.trim().isEmpty();
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
        allMetadata.put("severity", severity.name());
        allMetadata.put("breakdownType", breakdownType.name());
        allMetadata.put("reportedBy", reportedBy);
        allMetadata.put("isCriticalEmergency", isCriticalEmergency());
        allMetadata.put("requiresEvacuation", requiresPassengerEvacuation());
        allMetadata.put("shouldArrangeTransport", shouldArrangeAlternativeTransport());
        allMetadata.put("requiresTrafficControl", requiresTrafficControl());
        allMetadata.put("requiresImmediateMaintenance", requiresImmediateMaintenance());
        allMetadata.put("responseUrgency", getResponseUrgency().name());
        allMetadata.put("processingPriority", getProcessingPriority());
        allMetadata.put("shouldNotifyEmergency", shouldNotifyEmergencyServices());
        allMetadata.put("shouldSuspendRoute", shouldSuspendRoute());
        allMetadata.put("estimatedRepairTime", getEstimatedRepairTime().name());
        allMetadata.put("hasAdequateInfo", hasAdequateInformation());
        allMetadata.put("passengersOnBoard", passengersOnBoard);
        allMetadata.put("blocksTraffic", blocksTraffic);
        allMetadata.put("requiresEmergencyServices", requiresEmergencyServices);

        if (routeId != null) {
            allMetadata.put("routeId", routeId);
        }
        if (routeName != null) {
            allMetadata.put("routeName", routeName);
        }
        if (estimatedPassengerCount != null) {
            allMetadata.put("estimatedPassengerCount", estimatedPassengerCount);
        }
        if (nearestLandmark != null) {
            allMetadata.put("nearestLandmark", nearestLandmark);
        }
        if (temperatureC != null) {
            allMetadata.put("temperatureC", temperatureC);
        }
        if (previousStatus != null) {
            allMetadata.put("previousStatus", previousStatus.name());
        }

        return Map.copyOf(allMetadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        VehicleBreakdownReportedEvent that = (VehicleBreakdownReportedEvent) obj;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("VehicleBreakdownReportedEvent{eventId='%s', vehicleId=%s, severity=%s, type=%s, passengers=%s, timestamp=%s}",
                eventId.substring(0, 8) + "...",
                vehicleId,
                severity,
                breakdownType,
                passengersOnBoard ? estimatedPassengerCount + " pax" : "no pax",
                occurredAt
        );
    }

    @Getter
    public enum BreakdownSeverity {
        LOW("Low", "green"),
        MEDIUM("Medium", "yellow"),
        HIGH("High", "orange"),
        CRITICAL("Critical", "red");

        private final String description;
        private final String colorCode;

        BreakdownSeverity(String description, String colorCode) {
            this.description = description;
            this.colorCode = colorCode;
        }

    }

    @Getter
    public enum BreakdownType {
        ENGINE_FAILURE("Engine Failure"),
        TRANSMISSION_FAULT("Transmission Fault"),
        ELECTRICAL_FAULT("Electrical Fault"),
        BRAKE_ISSUE("Brake Issue"),
        TIRE_PUNCTURE("Tire Puncture"),
        DOOR_MALFUNCTION("Door Malfunction"),
        FIRE("Fire"),
        ACCIDENT("Accident"),
        MINOR_MECHANICAL("Minor Mechanical"),
        UNKNOWN("Unknown");

        private final String description;

        BreakdownType(String description) {
            this.description = description;
        }

    }

    @Getter
    public enum ResponseUrgency {
        IMMEDIATE("Immediate", 5),
        URGENT("Urgent", 15),
        PRIORITY("Priority", 30),
        NORMAL("Normal", 120);

        private final String description;
        private final int maxResponseTimeMinutes;

        ResponseUrgency(String description, int maxResponseTimeMinutes) {
            this.description = description;
            this.maxResponseTimeMinutes = maxResponseTimeMinutes;
        }

    }

    @Getter
    public enum EstimatedRepairTime {
        MINUTES("Minutes", 60),
        MINUTES_TO_HOUR("Minutes to Hour", 90),
        HOURS("Hours", 480),
        HOURS_TO_DAYS("Hours to Days", 1440),
        DAYS_TO_WEEKS("Days to Weeks", 10080),
        UNKNOWN("Unknown", -1);

        private final String description;
        private final int maxMinutes;

        EstimatedRepairTime(String description, int maxMinutes) {
            this.description = description;
            this.maxMinutes = maxMinutes;
        }

    }
}