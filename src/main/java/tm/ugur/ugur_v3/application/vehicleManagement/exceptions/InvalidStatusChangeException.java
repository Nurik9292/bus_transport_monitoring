package tm.ugur.ugur_v3.application.vehicleManagement.exceptions;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

@Getter
public class InvalidStatusChangeException  extends VehicleManagementException {

    private final VehicleStatus currentStatus;
    private final VehicleStatus requestedStatus;

    public InvalidStatusChangeException(String message, VehicleId vehicleId,
                                        VehicleStatus currentStatus,
                                        VehicleStatus requestedStatus) {
        super(String.format("%s (current: %s, requested: %s)", message, currentStatus, requestedStatus),
                "INVALID_STATUS_CHANGE", vehicleId);
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
    }

    @Override
    public String getErrorCategory() {
        return "BUSINESS_RULE_ERROR";
    }

}