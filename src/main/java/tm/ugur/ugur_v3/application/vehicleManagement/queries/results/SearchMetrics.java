package tm.ugur.ugur_v3.application.vehicleManagement.queries.results;

import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record SearchMetrics(
        Map<VehicleType, Long> vehiclesByType,
        Map<VehicleStatus, Long> vehiclesByStatus,
        int vehiclesWithGps,
        int vehiclesWithoutGps,
        double averageCapacity,
        int totalCapacity
) {

    public static SearchMetrics calculate(List<Vehicle> vehicles) {
        if (vehicles.isEmpty()) {
            return new SearchMetrics(
                    Map.of(),
                    Map.of(),
                    0, 0, 0.0, 0
            );
        }


        Map<VehicleType, Long> byType = vehicles.stream()
                .collect(Collectors.groupingBy(
                        Vehicle::getVehicleType,
                        Collectors.counting()
                ));


        Map<VehicleStatus, Long> byStatus = vehicles.stream()
                .collect(Collectors.groupingBy(
                        Vehicle::getStatus,
                        Collectors.counting()
                ));


        long withGps = vehicles.stream()
                .filter(Vehicle::hasRecentGpsData)
                .count();
        int withoutGps = vehicles.size() - (int) withGps;


        double avgCapacity = vehicles.stream()
                .filter(v -> v.getCapacity() != null)
                .mapToInt(v -> v.getCapacity().getTotalCapacity())
                .average()
                .orElse(0.0);


        int totalCap = vehicles.stream()
                .filter(v -> v.getCapacity() != null)
                .mapToInt(v -> v.getCapacity().getTotalCapacity())
                .sum();

        return new SearchMetrics(
                byType,
                byStatus,
                (int) withGps,
                withoutGps,
                avgCapacity,
                totalCap
        );
    }


    public boolean hasAdequateGpsCoverage() {
        int total = vehiclesWithGps + vehiclesWithoutGps;
        if (total == 0) return true;

        double gpsPercentage = (double) vehiclesWithGps / total;
        return gpsPercentage >= 0.8;
    }

    public int getTotalVehicles() {
        return vehiclesWithGps + vehiclesWithoutGps;
    }

    public double getGpsPercentage() {
        int total = getTotalVehicles();
        return total > 0 ? (double) vehiclesWithGps / total * 100.0 : 0.0;
    }

    public VehicleType getMostCommonType() {
        return vehiclesByType.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public VehicleStatus getMostCommonStatus() {
        return vehiclesByStatus.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public String getMetricsSummary() {
        return String.format("Types: %d, GPS: %.1f%%, AvgCap: %.0f, Total: %d vehicles",
                vehiclesByType.size(),
                getGpsPercentage(),
                averageCapacity,
                getTotalVehicles());
    }
}