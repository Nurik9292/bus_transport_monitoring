package tm.ugur.ugur_v3.domain.vehicleManagement.exceptions;

import tm.ugur.ugur_v3.domain.shared.exceptions.DomainException;

public class VehicleAlreadyExistsException extends DomainException {
    public VehicleAlreadyExistsException(String vehicleNumber) {
        super("Vehicle already exists: " + vehicleNumber, "VEHICLE_ALREADY_EXISTS");
    }
}
