package tm.ugur.ugur_v3.application.vehicleManagement.queries.results;

import lombok.Builder;
import tm.ugur.ugur_v3.application.shared.commands.QueryResult;
import tm.ugur.ugur_v3.application.shared.pagination.PageRequest;
import tm.ugur.ugur_v3.application.shared.pagination.PageResult;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Builder
public record FindAvailableVehiclesResult(
        PageResult<Vehicle> vehicles,
        int totalAvailableCount,
        Duration queryTime,
        boolean fromCache,
        Instant timestamp,
        Optional<VehicleType> filteredByType,
        Optional<Integer> filteredByCapacity,
        Optional<String> searchLocation,
        Map<String, Object> searchMetadata
) implements QueryResult {



    public static FindAvailableVehiclesResult of(PageResult<Vehicle> vehicles,
                                                 int totalAvailableCount,
                                                 Duration queryTime,
                                                 boolean fromCache) {
        return FindAvailableVehiclesResult.builder()
                .vehicles(vehicles)
                .totalAvailableCount(totalAvailableCount)
                .queryTime(queryTime)
                .fromCache(fromCache)
                .timestamp(Instant.now())
                .filteredByType(Optional.empty())
                .filteredByCapacity(Optional.empty())
                .searchLocation(Optional.empty())
                .searchMetadata(Map.of())
                .build();
    }

    public static FindAvailableVehiclesResult ofWithFilters(PageResult<Vehicle> vehicles,
                                                            int totalAvailableCount,
                                                            Duration queryTime,
                                                            boolean fromCache,
                                                            VehicleType vehicleType,
                                                            Integer minimumCapacity) {
        return FindAvailableVehiclesResult.builder()
                .vehicles(vehicles)
                .totalAvailableCount(totalAvailableCount)
                .queryTime(queryTime)
                .fromCache(fromCache)
                .timestamp(Instant.now())
                .filteredByType(Optional.ofNullable(vehicleType))
                .filteredByCapacity(Optional.ofNullable(minimumCapacity))
                .searchLocation(Optional.empty())
                .searchMetadata(Map.of(
                        "hasTypeFilter", vehicleType != null,
                        "hasCapacityFilter", minimumCapacity != null
                ))
                .build();
    }

    public static FindAvailableVehiclesResult ofLocationBased(PageResult<Vehicle> vehicles,
                                                              int totalAvailableCount,
                                                              Duration queryTime,
                                                              boolean fromCache,
                                                              String searchLocation,
                                                              double radiusKm) {
        return FindAvailableVehiclesResult.builder()
                .vehicles(vehicles)
                .totalAvailableCount(totalAvailableCount)
                .queryTime(queryTime)
                .fromCache(fromCache)
                .timestamp(Instant.now())
                .filteredByType(Optional.empty())
                .filteredByCapacity(Optional.empty())
                .searchLocation(Optional.of(searchLocation))
                .searchMetadata(Map.of(
                        "isLocationBased", true,
                        "searchRadius", radiusKm,
                        "searchCenter", searchLocation
                ))
                .build();
    }

    public static FindAvailableVehiclesResult empty(Duration queryTime) {

        PageRequest defaultPageRequest = tm.ugur.ugur_v3.application.shared.pagination.PageRequestHelpers.forEmptyResult();

        return FindAvailableVehiclesResult.builder()
                .vehicles(PageResult.empty(defaultPageRequest))
                .totalAvailableCount(0)
                .queryTime(queryTime)
                .fromCache(false)
                .timestamp(Instant.now())
                .filteredByType(Optional.empty())
                .filteredByCapacity(Optional.empty())
                .searchLocation(Optional.empty())
                .searchMetadata(Map.of("isEmpty", true))
                .build();
    }

    public static FindAvailableVehiclesResult empty(Duration queryTime, PageRequest originalRequest) {

        return FindAvailableVehiclesResult.builder()
                .vehicles(PageResult.empty(originalRequest))
                .totalAvailableCount(0)
                .queryTime(queryTime)
                .fromCache(false)
                .timestamp(Instant.now())
                .filteredByType(Optional.empty())
                .filteredByCapacity(Optional.empty())
                .searchLocation(Optional.empty())
                .searchMetadata(Map.of(
                        "isEmpty", true,
                        "originalPageSize", originalRequest.getPageSize(),
                        "originalPageNumber", originalRequest.getPageNumber()
                ))
                .build();
    }

    public static FindAvailableVehiclesResult cached(PageResult<Vehicle> vehicles,
                                                     int totalAvailableCount,
                                                     Duration originalQueryTime) {
        return FindAvailableVehiclesResult.builder()
                .vehicles(vehicles)
                .totalAvailableCount(totalAvailableCount)
                .queryTime(Duration.ofMillis(1))
                .fromCache(true)
                .timestamp(Instant.now())
                .filteredByType(Optional.empty())
                .filteredByCapacity(Optional.empty())
                .searchLocation(Optional.empty())
                .searchMetadata(Map.of(
                        "originalQueryTime", originalQueryTime.toMillis(),
                        "cacheHit", true
                ))
                .build();
    }



    public boolean hasResults() {
        return vehicles != null && !vehicles.getContent().isEmpty();
    }

    public boolean isEmpty() {
        return !hasResults();
    }

    public int getResultCount() {
        return vehicles != null ? vehicles.getContent().size() : 0;
    }

    public List<Vehicle> getVehicles() {
        return vehicles != null ? vehicles.getContent() : List.of();
    }

    public boolean hasMorePages() {
        return vehicles != null && vehicles.hasNext();
    }

    public boolean hasFilters() {
        return filteredByType.isPresent() ||
                filteredByCapacity.isPresent() ||
                searchLocation.isPresent();
    }

    public boolean isLocationBasedSearch() {
        return searchLocation.isPresent();
    }

    public String getSearchSummary() {
        if (isEmpty()) {
            return "No available vehicles found";
        }

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Found %d vehicles", getResultCount()));

        if (totalAvailableCount > getResultCount()) {
            summary.append(String.format(" of %d total", totalAvailableCount));
        }

        if (hasFilters()) {
            summary.append(" (filtered");
            filteredByType.ifPresent(type -> summary.append(" type=").append(type));
            filteredByCapacity.ifPresent(cap -> summary.append(" capacity>=").append(cap));
            searchLocation.ifPresent(loc -> summary.append(" near ").append(loc));
            summary.append(")");
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
        Map<String, Object> metadata = Map.of(
                "resultType", "FindAvailableVehiclesResult",
                "resultCount", getResultCount(),
                "totalCount", totalAvailableCount,
                "hasMorePages", hasMorePages(),
                "fromCache", fromCache,
                "queryTime", queryTime.toMillis(),
                "timestamp", timestamp.toString(),
                "hasFilters", hasFilters(),
                "isLocationBased", isLocationBasedSearch()
        );


        if (!searchMetadata.isEmpty()) {
            Map<String, Object> combined = new java.util.HashMap<>(metadata);
            combined.putAll(searchMetadata);
            return Map.copyOf(combined);
        }

        return metadata;
    }

    @Override
    public boolean shouldCache() {
        return hasResults() && !isLocationBasedSearch();
    }



    public boolean isSlowQuery() {
        return queryTime.toMillis() > 100;
    }

    public boolean isFastQuery() {
        return queryTime.toMillis() <= 50;
    }

    public String getPerformanceCategory() {
        long millis = queryTime.toMillis();
        if (millis <= 50) return "FAST";
        if (millis <= 100) return "GOOD";
        if (millis <= 500) return "ACCEPTABLE";
        return "SLOW";
    }
}