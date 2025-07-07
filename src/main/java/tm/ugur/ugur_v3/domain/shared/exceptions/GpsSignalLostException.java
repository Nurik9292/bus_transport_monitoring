package tm.ugur.ugur_v3.domain.shared.exceptions;

public class GpsSignalLostException extends DomainException {
    public GpsSignalLostException(String vehicleId) {
        super("GPS signal lost for vehicle: " + vehicleId, "GPS_SIGNAL_LOST");
    }
}