package tm.ugur.ugur_v3.application.vehicleManagement.exceptions;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

@Getter
public class VehicleDataInconsistencyException extends VehicleManagementException {

    private final String inconsistencyType;
    private final String expectedValue;
    private final String actualValue;

    public VehicleDataInconsistencyException(String message, VehicleId vehicleId,
                                             String inconsistencyType,
                                             String expectedValue,
                                             String actualValue) {
        super(String.format("%s - Expected: %s, Actual: %s", message, expectedValue, actualValue),
                "DATA_INCONSISTENCY", vehicleId);
        this.inconsistencyType = inconsistencyType;
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
    }

    @Override
    public String getErrorCategory() {
        return "DATA_INTEGRITY_ERROR";
    }

}