package tm.ugur.ugur_v3.domain.vehicleManagement.exceptions;

import tm.ugur.ugur_v3.domain.shared.exceptions.DomainException;

public class VehicleNotFoundException extends DomainException {
    public VehicleNotFoundException(String vehicleId) {
        super("Vehicle not found: " + vehicleId, "VEHICLE_NOT_FOUND");
    }
}