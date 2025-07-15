package tm.ugur.ugur_v3.application.vehicleManagement.queries.results;

import lombok.Builder;
import tm.ugur.ugur_v3.application.shared.commands.QueryResult;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Builder
public record GetVehiclesNearLocationResult(
        GeoCoordinate searchLocation,
        double searchRadiusKm,
        List<VehicleNearLocationResult> nearbyVehicles,
        Duration queryTime,
        Instant timestamp,
        boolean includeDistance,
        boolean requireRecentGps,
        String errorMessage
) implements QueryResult {



    public static GetVehiclesNearLocationResult success(GeoCoordinate searchLocation,
                                                        double searchRadiusKm,
                                                        List<VehicleNearLocationResult> nearbyVehicles,
                                                        Duration queryTime,
                                                        boolean includeDistance,
                                                        boolean requireRecentGps) {
        return GetVehiclesNearLocationResult.builder()
                .searchLocation(searchLocation)
                .searchRadiusKm(searchRadiusKm)
                .nearbyVehicles(List.copyOf(nearbyVehicles))
                .queryTime(queryTime)
                .timestamp(Instant.now())
                .includeDistance(includeDistance)
                .requireRecentGps(requireRecentGps)
                .errorMessage(null)
                .build();
    }

    public static GetVehiclesNearLocationResult empty(GeoCoordinate searchLocation,
                                                      double searchRadiusKm,
                                                      Duration queryTime,
                                                      String reason) {
        return GetVehiclesNearLocationResult.builder()
                .searchLocation(searchLocation)
                .searchRadiusKm(searchRadiusKm)
                .nearbyVehicles(List.of())
                .queryTime(queryTime)
                .timestamp(Instant.now())
                .includeDistance(false)
                .requireRecentGps(false)
                .errorMessage(reason)
                .build();
    }



    public static GetVehiclesNearLocationResult error(GeoCoordinate searchLocation,
                                                      double searchRadiusKm,
                                                      String errorMessage,
                                                      Duration queryTime) {
        return GetVehiclesNearLocationResult.builder()
                .searchLocation(searchLocation)
                .searchRadiusKm(searchRadiusKm)
                .nearbyVehicles(List.of())
                .queryTime(queryTime)
                .timestamp(Instant.now())
                .includeDistance(false)
                .requireRecentGps(false)
                .errorMessage(errorMessage)
                .build();
    }



    public boolean hasResults() {
        return nearbyVehicles != null && !nearbyVehicles.isEmpty();
    }

    public boolean isEmpty() {
        return !hasResults();
    }

    public boolean hasError() {
        return errorMessage != null && !errorMessage.trim().isEmpty();
    }

    public int getResultCount() {
        return nearbyVehicles != null ? nearbyVehicles.size() : 0;
    }

    public double getSearchAreaKm2() {
        return Math.PI * searchRadiusKm * searchRadiusKm;
    }

    public boolean isWideAreaSearch() {
        return searchRadiusKm > 10.0;
    }

    public boolean isLocalSearch() {
        return searchRadiusKm <= 2.0;
    }

    public VehicleNearLocationResult getClosestVehicle() {
        if (isEmpty()) return null;

        return nearbyVehicles.stream()
                .min((a, b) -> Double.compare(a.distanceMeters(), b.distanceMeters()))
                .orElse(null);
    }

    public List<VehicleNearLocationResult> getVehiclesWithinDistance(double maxDistanceKm) {
        if (isEmpty()) return List.of();

        double maxDistanceMeters = maxDistanceKm * 1000.0;
        return nearbyVehicles.stream()
                .filter(vehicle -> vehicle.distanceMeters() <= maxDistanceMeters)
                .toList();
    }

    public double getAverageDistance() {
        if (isEmpty()) return 0.0;

        return nearbyVehicles.stream()
                .mapToDouble(VehicleNearLocationResult::distanceMeters)
                .average()
                .orElse(0.0);
    }

    public String getSearchSummary() {
        if (hasError()) {
            return String.format("Location search failed: %s", errorMessage);
        }

        if (isEmpty()) {
            return String.format("No vehicles found within %.1fkm of (%.6f, %.6f)",
                    searchRadiusKm,
                    searchLocation.getLatitude(),
                    searchLocation.getLongitude());
        }

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Found %d vehicles within %.1fkm", getResultCount(), searchRadiusKm));

        VehicleNearLocationResult closest = getClosestVehicle();
        if (closest != null) {
            summary.append(String.format(" (closest: %.0fm away)", closest.distanceMeters()));
        }

        return summary.toString();
    }



    @Override
    public boolean isFromCache() {
        return false;
    }

    @Override
    public Duration getQueryTime() {
        return queryTime;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = Map.ofEntries(
                Map.entry("resultType", "GetVehiclesNearLocationResult"),
                Map.entry("searchLatitude", searchLocation.getLatitude()),
                Map.entry("searchLongitude", searchLocation.getLongitude()),
                Map.entry("searchRadiusKm", searchRadiusKm),
                Map.entry("searchAreaKm2", getSearchAreaKm2()),
                Map.entry("resultCount", getResultCount()),
                Map.entry("queryTime", queryTime.toMillis()),
                Map.entry("timestamp", timestamp.toString()),
                Map.entry("includeDistance", includeDistance),
                Map.entry("requireRecentGps", requireRecentGps),
                Map.entry("hasError", hasError()),
                Map.entry("isWideAreaSearch", isWideAreaSearch()),
                Map.entry("isLocalSearch", isLocalSearch())
        );

        if (hasResults()) {
            Map<String, Object> extended = new java.util.HashMap<>(metadata);
            extended.put("averageDistanceMeters", getAverageDistance());

            VehicleNearLocationResult closest = getClosestVehicle();
            if (closest != null) {
                extended.put("closestDistanceMeters", closest.distanceMeters());
            }

            return Map.copyOf(extended);
        }

        return metadata;
    }

    @Override
    public boolean shouldCache() {
        return false;
    }



    public boolean isSlowQuery() {
        return queryTime.toMillis() > 300;
    }

    public boolean isFastQuery() {
        return queryTime.toMillis() <= 150;
    }

    public String getPerformanceCategory() {
        long millis = queryTime.toMillis();
        if (millis <= 100) return "FAST";
        if (millis <= 150) return "GOOD";
        if (millis <= 300) return "ACCEPTABLE";
        return "SLOW";
    }



    public boolean hasVehiclesInRadius(double radiusKm) {
        return !getVehiclesWithinDistance(radiusKm).isEmpty();
    }

    public int countVehiclesInRadius(double radiusKm) {
        return getVehiclesWithinDistance(radiusKm).size();
    }

    public double getDensityPerKm2() {
        if (isEmpty()) return 0.0;
        return getResultCount() / getSearchAreaKm2();
    }


    public String getDebugInfo() {
        return String.format("GetVehiclesNearLocationResult{location=(%.6f,%.6f), radius=%.1fkm, found=%d, time=%dms, hasError=%s}",
                searchLocation.getLatitude(),
                searchLocation.getLongitude(),
                searchRadiusKm,
                getResultCount(),
                queryTime.toMillis(),
                hasError());
    }
}