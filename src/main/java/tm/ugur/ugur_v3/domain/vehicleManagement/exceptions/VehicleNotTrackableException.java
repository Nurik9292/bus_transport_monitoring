package tm.ugur.ugur_v3.domain.vehicleManagement.exceptions;

import tm.ugur.ugur_v3.domain.shared.exceptions.DomainException;

public class VehicleNotTrackableException extends DomainException {


    protected VehicleNotTrackableException(String message) {
        super(message, "VEHICLE_NOT_TRACKABLE");
    }
}
