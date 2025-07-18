package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import java.util.Map;

public  record VehicleAvailability(
        int totalVehicles,
        int peakHourVehicles,
        int maintenanceVehicles,
        Map<String, Integer> vehicleTypeAvailability
) {}