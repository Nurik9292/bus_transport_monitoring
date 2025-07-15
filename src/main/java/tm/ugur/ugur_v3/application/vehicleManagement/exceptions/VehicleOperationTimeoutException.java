package tm.ugur.ugur_v3.application.vehicleManagement.exceptions;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

@Getter
public class VehicleOperationTimeoutException  extends VehicleManagementException {

    private final java.time.Duration timeout;
    private final String operation;

    public VehicleOperationTimeoutException(String operation, java.time.Duration timeout, VehicleId vehicleId) {
        super(String.format("Operation '%s' timed out after %d seconds", operation, timeout.getSeconds()),
                "OPERATION_TIMEOUT", vehicleId);
        this.timeout = timeout;
        this.operation = operation;
    }

    @Override
    public String getErrorCategory() {
        return "PERFORMANCE_ERROR";
    }

}