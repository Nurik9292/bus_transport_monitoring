package tm.ugur.ugur_v3.domain.routeManagement.events;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

import java.util.Map;

@Getter
public final class VehicleAssignedToRouteEvent extends BaseRouteEvent {

    private final VehicleId vehicleId;
    private final String assignedBy;
    private final String shiftType;
    private final boolean isTemporaryAssignment;

    private VehicleAssignedToRouteEvent(RouteId routeId, VehicleId vehicleId, String assignedBy,
                                        String shiftType, boolean isTemporaryAssignment,
                                        String correlationId, Map<String, Object> metadata) {
        super("VehicleAssignedToRoute", routeId, correlationId, metadata);
        this.vehicleId = vehicleId;
        this.assignedBy = assignedBy;
        this.shiftType = shiftType;
        this.isTemporaryAssignment = isTemporaryAssignment;
    }

    public static VehicleAssignedToRouteEvent of(RouteId routeId, VehicleId vehicleId) {
        return new VehicleAssignedToRouteEvent(routeId, vehicleId, "SYSTEM", "REGULAR",
                false, null, null);
    }

    public static VehicleAssignedToRouteEvent temporary(RouteId routeId, VehicleId vehicleId, String assignedBy) {
        return new VehicleAssignedToRouteEvent(routeId, vehicleId, assignedBy, "TEMPORARY",
                true, null, null);
    }

    @Override
    public String toString() {
        return String.format("VehicleAssignedToRouteEvent{routeId=%s, vehicleId=%s, temporary=%s}",
                routeId, vehicleId, isTemporaryAssignment);
    }
}