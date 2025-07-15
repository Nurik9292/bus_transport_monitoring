package tm.ugur.ugur_v3.application.vehicleManagement.queries.results;

import lombok.Builder;
import tm.ugur.ugur_v3.application.shared.commands.QueryResult;
import tm.ugur.ugur_v3.application.shared.pagination.PageResult;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Builder
public record FindVehiclesByStatusResult(
        VehicleStatus searchedStatus,
        PageResult<Vehicle> vehicles,
        boolean fromCache,
        Duration queryTime,
        Instant timestamp,
        boolean includeLocationData,
        boolean requireRecentGps,
        String errorMessage
) implements QueryResult {



    public static FindVehiclesByStatusResult success(VehicleStatus status,
                                                     PageResult<Vehicle> vehicles,
                                                     boolean fromCache,
                                                     Duration queryTime,
                                                     boolean includeLocationData,
                                                     boolean requireRecentGps) {
        return FindVehiclesByStatusResult.builder()
                .searchedStatus(status)
                .vehicles(vehicles)
                .fromCache(fromCache)
                .queryTime(queryTime)
                .timestamp(Instant.now())
                .includeLocationData(includeLocationData)
                .requireRecentGps(requireRecentGps)
                .errorMessage(null)
                .build();
    }

    public static FindVehiclesByStatusResult cached(VehicleStatus status,
                                                    PageResult<Vehicle> vehicles,
                                                    Duration originalQueryTime,
                                                    boolean includeLocationData,
                                                    boolean requireRecentGps) {
        return FindVehiclesByStatusResult.builder()
                .searchedStatus(status)
                .vehicles(vehicles)
                .fromCache(true)
                .queryTime(Duration.ofMillis(1))
                .timestamp(Instant.now())
                .includeLocationData(includeLocationData)
                .requireRecentGps(requireRecentGps)
                .errorMessage(null)
                .build();
    }



    public static FindVehiclesByStatusResult error(VehicleStatus status,
                                                   String errorMessage,
                                                   Duration queryTime) {
        return FindVehiclesByStatusResult.builder()
                .searchedStatus(status)
                .vehicles(null)
                .fromCache(false)
                .queryTime(queryTime)
                .timestamp(Instant.now())
                .includeLocationData(false)
                .requireRecentGps(false)
                .errorMessage(errorMessage)
                .build();
    }

    public static FindVehiclesByStatusResult empty(VehicleStatus status,
                                                   Duration queryTime,
                                                   tm.ugur.ugur_v3.application.shared.pagination.PageRequest pageRequest) {
        PageResult<Vehicle> emptyResult = PageResult.of(java.util.List.of(), pageRequest, 0);

        return FindVehiclesByStatusResult.builder()
                .searchedStatus(status)
                .vehicles(emptyResult)
                .fromCache(false)
                .queryTime(queryTime)
                .timestamp(Instant.now())
                .includeLocationData(false)
                .requireRecentGps(false)
                .errorMessage(null)
                .build();
    }



    public boolean hasResults() {
        return vehicles != null && !vehicles.getContent().isEmpty();
    }

    public boolean isEmpty() {
        return !hasResults();
    }

    public boolean hasError() {
        return errorMessage != null && !errorMessage.trim().isEmpty();
    }

    public int getResultCount() {
        return vehicles != null ? vehicles.getContent().size() : 0;
    }

    public long getTotalCount() {
        return vehicles != null ? vehicles.getTotalElements() : 0;
    }

    public boolean hasMorePages() {
        return vehicles != null && vehicles.hasNext();
    }

    public boolean isLocationDataIncluded() {
        return includeLocationData;
    }

    public boolean isGpsDataRequired() {
        return requireRecentGps;
    }

    public String getSearchSummary() {
        if (hasError()) {
            return String.format("Search failed for status %s: %s", searchedStatus.name(), errorMessage);
        }

        if (isEmpty()) {
            return String.format("No vehicles found with status %s", searchedStatus.name());
        }

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Found %d vehicles with status %s", getResultCount(), searchedStatus.name()));

        if (getTotalCount() > getResultCount()) {
            summary.append(String.format(" (total: %d)", getTotalCount()));
        }

        if (isLocationDataIncluded()) {
            summary.append(" with location data");
        }

        if (isGpsDataRequired()) {
            summary.append(" with recent GPS");
        }

        return summary.toString();
    }



    @Override
    public boolean isFromCache() {
        return fromCache;
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
        return Map.ofEntries(
                Map.entry("resultType", "FindVehiclesByStatusResult"),
                Map.entry("searchedStatus", searchedStatus.name()),
                Map.entry("resultCount", getResultCount()),
                Map.entry("totalCount", getTotalCount()),
                Map.entry("hasMorePages", hasMorePages()),
                Map.entry("fromCache", fromCache),
                Map.entry("queryTime", queryTime.toMillis()),
                Map.entry("timestamp", timestamp.toString()),
                Map.entry("includeLocationData", includeLocationData),
                Map.entry("requireRecentGps", requireRecentGps),
                Map.entry("hasError", hasError())
        );
    }

    @Override
    public boolean shouldCache() {
        return hasResults() && !hasError() && isCacheableStatus();
    }



    public boolean isSlowQuery() {
        return queryTime.toMillis() > 200;
    }

    public boolean isFastQuery() {
        return queryTime.toMillis() <= 100;
    }

    public String getPerformanceCategory() {
        if (fromCache) return "CACHED";

        long millis = queryTime.toMillis();
        if (millis <= 50) return "FAST";
        if (millis <= 100) return "GOOD";
        if (millis <= 200) return "ACCEPTABLE";
        return "SLOW";
    }



    private boolean isCacheableStatus() {

        return searchedStatus == VehicleStatus.AT_DEPOT ||
                searchedStatus == VehicleStatus.MAINTENANCE ||
                searchedStatus == VehicleStatus.RETIRED ||
                searchedStatus == VehicleStatus.INACTIVE;
    }


    public String getDebugInfo() {
        return String.format("FindVehiclesByStatusResult{status=%s, found=%d, cache=%s, time=%dms, hasError=%s}",
                searchedStatus.name(),
                getResultCount(),
                fromCache,
                queryTime.toMillis(),
                hasError());
    }
}