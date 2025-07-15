package tm.ugur.ugur_v3.application.vehicleManagement.exceptions;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

@Getter
public class InvalidGpsDataException extends VehicleManagementException {

    private final double latitude;
    private final double longitude;
    private final double accuracy;

    public InvalidGpsDataException(String message, VehicleId vehicleId,
                                   double latitude, double longitude, double accuracy) {
        super(String.format("%s (lat=%.6f, lng=%.6f, acc=%.1fm)", message, latitude, longitude, accuracy),
                "INVALID_GPS_DATA", vehicleId);
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
    }

    public InvalidGpsDataException(String message, VehicleId vehicleId) {
        super(message, "INVALID_GPS_DATA", vehicleId);
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.accuracy = 0.0;
    }

    @Override
    public String getErrorCategory() {
        return "GPS_DATA_ERROR";
    }

}