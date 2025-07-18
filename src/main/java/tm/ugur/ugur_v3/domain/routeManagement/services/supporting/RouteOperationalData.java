package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;


/**
 * Операционные данные маршрута
 * TODO: Реализовать в Infrastructure слое при интеграции с операционными системами
 */
public record RouteOperationalData(
        RouteId routeId,
        int activeVehicles,
        double averageSpeed,
        int currentPassengers,
        double fuelConsumption,
        int breakdowns,
        double revenue,
        Timestamp dataTimestamp
) {

    public static RouteOperationalData empty(RouteId routeId) {
        return new RouteOperationalData(
                routeId,
                0,    // active vehicles
                0.0,  // average speed
                0,    // current passengers
                0.0,  // fuel consumption
                0,    // breakdowns
                0.0,  // revenue
                Timestamp.now()
        );
    }

    public boolean hasOperationalIssues() {
        return breakdowns > 0 || averageSpeed < 15.0;
    }

    public double getRevenuePerVehicle() {
        return activeVehicles > 0 ? revenue / activeVehicles : 0.0;
    }

    public boolean isOperational() {
        return activeVehicles > 0;
    }
}