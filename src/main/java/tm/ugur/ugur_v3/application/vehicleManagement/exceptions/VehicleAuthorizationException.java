package tm.ugur.ugur_v3.application.vehicleManagement.exceptions;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

@Getter
public class VehicleAuthorizationException extends VehicleManagementException {

    private final String requiredPermission;
    private final String userRole;

    public VehicleAuthorizationException(String message, VehicleId vehicleId,
                                         String requiredPermission, String userRole) {
        super(String.format("%s - Required: %s, User role: %s", message, requiredPermission, userRole),
                "AUTHORIZATION_FAILED", vehicleId);
        this.requiredPermission = requiredPermission;
        this.userRole = userRole;
    }

    @Override
    public String getErrorCategory() {
        return "SECURITY_ERROR";
    }

}