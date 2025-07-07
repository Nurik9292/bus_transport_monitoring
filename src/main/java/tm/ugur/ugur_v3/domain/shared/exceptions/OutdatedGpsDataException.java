package tm.ugur.ugur_v3.domain.shared.exceptions;

import java.time.LocalDateTime;

public class OutdatedGpsDataException extends DomainException {
    public OutdatedGpsDataException(String vehicleId, LocalDateTime lastUpdate) {
        super("Outdated GPS data for vehicle: " + vehicleId + ", last update: " + lastUpdate,
                "OUTDATED_GPS_DATA");
    }
}
