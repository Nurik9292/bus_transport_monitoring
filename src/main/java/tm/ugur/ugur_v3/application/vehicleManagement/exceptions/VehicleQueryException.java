package tm.ugur.ugur_v3.application.vehicleManagement.exceptions;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

@Getter
public class VehicleQueryException extends VehicleManagementException {

    private final String queryType;
    private final java.time.Duration queryTime;

    public VehicleQueryException(String message, String queryType, VehicleId vehicleId) {
        super(message, "QUERY_FAILED", vehicleId);
        this.queryType = queryType;
        this.queryTime = java.time.Duration.ZERO;
    }

    public VehicleQueryException(String message, String queryType, VehicleId vehicleId,
                                 java.time.Duration queryTime, Throwable cause) {
        super(String.format("%s (query time: %dms)", message, queryTime.toMillis()),
                "QUERY_FAILED", vehicleId, cause);
        this.queryType = queryType;
        this.queryTime = queryTime;
    }

    @Override
    public String getErrorCategory() {
        return "QUERY_ERROR";
    }

}