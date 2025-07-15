package tm.ugur.ugur_v3.application.vehicleManagement.commands;

import org.springframework.stereotype.Component;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.vehicleManagement.exceptions.InvalidGpsDataException;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.Bearing;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.Speed;

@Component
public class GpsDataValidator {

    private static final double MAX_SPEED_CHANGE_PER_SECOND = 10.0;
    private static final double MAX_LOCATION_JUMP_METERS = 1000.0;

    public void validateGpsData(GeoCoordinate location, Speed speed, Bearing bearing) {
        if (!isValidGpsCoordinate(location)) {
            throw new InvalidGpsDataException("GPS coordinates are invalid or unrealistic");
        }

        if (!isRealisticSpeed(speed)) {
            throw new InvalidGpsDataException("Speed is unrealistic: " + speed.getKmh() + " km/h");
        }

        // Additional validation could include:
        // - Speed change rate validation
        // - Location jump detection
        // - Route adherence checks
    }

    private boolean isValidGpsCoordinate(GeoCoordinate location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            return false;
        }

        // Check if coordinates are not null island (0,0) unless actually there
        if (lat == 0.0 && lon == 0.0) {
            return false;
        }

        return true;
    }

    private boolean isRealisticSpeed(Speed speed) {
        return speed.getKmh() >= 0.0 && speed.getKmh() <= 120.0;
    }
}