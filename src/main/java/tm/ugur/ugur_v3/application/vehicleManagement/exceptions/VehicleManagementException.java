package tm.ugur.ugur_v3.application.vehicleManagement.exceptions;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

@Getter
public abstract class VehicleManagementException extends RuntimeException {

    private final String errorCode;
    private final VehicleId vehicleId;

    protected VehicleManagementException(String message, String errorCode, VehicleId vehicleId) {
        super(message);
        this.errorCode = errorCode;
        this.vehicleId = vehicleId;
    }

    protected VehicleManagementException(String message, String errorCode, VehicleId vehicleId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.vehicleId = vehicleId;
    }


    public abstract String getErrorCategory();
}