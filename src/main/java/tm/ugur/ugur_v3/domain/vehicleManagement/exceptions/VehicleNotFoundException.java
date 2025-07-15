package tm.ugur.ugur_v3.domain.vehicleManagement.exceptions;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.DomainException;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

import java.util.Map;

@Getter
public final class VehicleNotFoundException extends DomainException {

    private final VehicleId vehicleId;

    public VehicleNotFoundException(VehicleId vehicleId) {
        super(
                String.format("Vehicle not found: %s", vehicleId.getValue()),
                "VEHICLE_NOT_FOUND",
                Map.of(
                        "vehicleId", vehicleId.getValue(),
                        "searchType", "BY_ID"
                )
        );
        this.vehicleId = vehicleId;
    }

    public VehicleNotFoundException(String licensePlate) {
        super(
                String.format("Vehicle not found by license plate: %s", licensePlate),
                "VEHICLE_NOT_FOUND",
                Map.of(
                        "licensePlate", licensePlate,
                        "searchType", "BY_LICENSE_PLATE"
                )
        );
        this.vehicleId = null;
    }

    public VehicleNotFoundException(VehicleId vehicleId, String additionalContext) {
        super(
                String.format("Vehicle not found: %s (%s)", vehicleId.getValue(), additionalContext),
                "VEHICLE_NOT_FOUND",
                Map.of(
                        "vehicleId", vehicleId.getValue(),
                        "searchType", "BY_ID",
                        "context", additionalContext
                )
        );
        this.vehicleId = vehicleId;
    }

    public static VehicleNotFoundException byId(VehicleId vehicleId) {
        return new VehicleNotFoundException(vehicleId);
    }

    public static VehicleNotFoundException byLicensePlate(String licensePlate) {
        return new VehicleNotFoundException(licensePlate);
    }

    public static VehicleNotFoundException byIdWithContext(VehicleId vehicleId, String context) {
        return new VehicleNotFoundException(vehicleId, context);
    }

    public String getSearchType() {
        return (String) getContext().get("searchType");
    }

    public boolean isSearchById() {
        return "BY_ID".equals(getSearchType());
    }

    public boolean isSearchByLicensePlate() {
        return "BY_LICENSE_PLATE".equals(getSearchType());
    }
}