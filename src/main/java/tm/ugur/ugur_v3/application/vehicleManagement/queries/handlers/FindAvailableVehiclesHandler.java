package tm.ugur.ugur_v3.application.vehicleManagement.queries.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.application.shared.caching.CacheKey;
import tm.ugur.ugur_v3.application.shared.caching.CacheManager;
import tm.ugur.ugur_v3.application.shared.executor.UseCaseExecutor;
import tm.ugur.ugur_v3.application.shared.monitoring.PerformanceMonitor;
import tm.ugur.ugur_v3.application.shared.pagination.PageResult;
import tm.ugur.ugur_v3.application.shared.validation.ValidatorService;
import tm.ugur.ugur_v3.application.vehicleManagement.queries.FindAvailableVehiclesQuery;
import tm.ugur.ugur_v3.application.vehicleManagement.queries.results.FindAvailableVehiclesResult;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;
import tm.ugur.ugur_v3.domain.vehicleManagement.repository.VehicleRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FindAvailableVehiclesHandler implements UseCaseExecutor.QueryHandler<FindAvailableVehiclesQuery, Mono<FindAvailableVehiclesResult>> {

    private final VehicleRepository vehicleRepository;
    private final CacheManager cacheManager;
    private final PerformanceMonitor performanceMonitor;
    private final ValidatorService validatorService;

    @Override
    public Mono<FindAvailableVehiclesResult> handle(FindAvailableVehiclesQuery query) {
        Instant startTime = Instant.now();

        return performanceMonitor.timeReactive("vehicle.search.available", () ->
                processAvailableVehiclesSearch(query, startTime)
        );
    }

    private Mono<FindAvailableVehiclesResult> processAvailableVehiclesSearch(FindAvailableVehiclesQuery query,
                                                                             Instant startTime) {
        return validateQuery(query)
                .then(tryFromCacheIfEnabled(query, startTime))
                .switchIfEmpty(searchFromRepository(query, startTime))
                .doOnSuccess(result -> logSearchResult(query, result))
                .onErrorResume(throwable -> handleSearchError(query, throwable, startTime));
    }

    private Mono<Void> validateQuery(FindAvailableVehiclesQuery query) {
        return Mono.fromRunnable(() -> {
            try {
                validatorService.validate(query);
                log.debug("Available vehicles search query validation passed");
            } catch (Exception e) {
                log.warn("Available vehicles search query validation failed: {}", e.getMessage());
                throw new IllegalArgumentException("Query validation failed: " + e.getMessage(), e);
            }
        });
    }

    private Mono<FindAvailableVehiclesResult> tryFromCacheIfEnabled(FindAvailableVehiclesQuery query,
                                                                    Instant startTime) {
        if (!query.isCacheable()) {
            return Mono.empty();
        }

        String cacheKeyStr = buildCacheKey(query);
        CacheKey cacheKey = CacheKey.of("vehicle", "search", cacheKeyStr);

        return Mono.fromCallable(() ->
                        cacheManager.get(cacheKey, () -> null, Duration.ofMinutes(5))
                )
                .cast(FindAvailableVehiclesResult.class)
                .map(cachedResult -> {
                    log.debug("Available vehicles search found in cache");
                    performanceMonitor.incrementCounter("vehicle.search.cache.hit");

                    Duration queryTime = Duration.between(startTime, Instant.now());
                    return FindAvailableVehiclesResult.cached(
                            cachedResult.vehicles(),
                            cachedResult.totalAvailableCount(),
                            queryTime
                    );
                })
                .doOnError(error -> {
                    log.warn("Cache lookup failed: {}", error.getMessage());
                    performanceMonitor.incrementCounter("vehicle.search.cache.error");
                })
                .onErrorResume(throwable -> Mono.empty());
    }

    private Mono<FindAvailableVehiclesResult> searchFromRepository(FindAvailableVehiclesQuery query,
                                                                   Instant startTime) {
        return findAvailableVehicles(query)
                .collectList()
                .map(vehicles -> {
                    Duration queryTime = Duration.between(startTime, Instant.now());
                    return createSearchResult(vehicles, query, queryTime, false);
                })
                .doOnSuccess(result -> {
                    if (query.isCacheable() && !query.isLocationBasedSearch()) {
                        cacheSearchResult(query, result);
                    }
                    performanceMonitor.incrementCounter("vehicle.search.repository.hit");
                });
    }

    private FindAvailableVehiclesResult createSearchResult(List<Vehicle> vehicles,
                                                           FindAvailableVehiclesQuery query,
                                                           Duration queryTime,
                                                           boolean fromCache) {

        PageResult<Vehicle> pageResult = applyPagination(vehicles, query.pageRequest());


        if (query.isLocationBasedSearch()) {
            return FindAvailableVehiclesResult.ofLocationBased(
                    pageResult,
                    vehicles.size(),
                    queryTime,
                    fromCache,
                    formatLocationSearch(query),
                    query.maxDistanceKm()
            );
        } else if (query.hasFiltersApplied()) {
            return FindAvailableVehiclesResult.ofWithFilters(
                    pageResult,
                    vehicles.size(),
                    queryTime,
                    fromCache,
                    query.vehicleType().orElse(null),
                    query.minimumCapacity().orElse(null)
            );
        } else {
            return FindAvailableVehiclesResult.of(
                    pageResult,
                    vehicles.size(),
                    queryTime,
                    fromCache
            );
        }
    }

    private String formatLocationSearch(FindAvailableVehiclesQuery query) {
        Optional<GeoCoordinate> location = query.nearLocation();
        if (location.isPresent()) {
            GeoCoordinate coord = location.get();
            return String.format("%.6f,%.6f", coord.getLatitude(), coord.getLongitude());
        }
        return "unknown";
    }

    private Flux<Vehicle> findAvailableVehicles(FindAvailableVehiclesQuery query) {
        return vehicleRepository.findAll()
                .filter(this::isVehicleAvailable)
                .filter(vehicle -> matchesVehicleTypeFilter(vehicle, query))
                .filter(vehicle -> matchesCapacityFilter(vehicle, query))
                .filter(vehicle -> matchesLocationFilter(vehicle, query))
                .filter(vehicle -> matchesGpsFilter(vehicle, query));
    }

    private boolean isVehicleAvailable(Vehicle vehicle) {
        VehicleStatus status = vehicle.getStatus();
        return status == VehicleStatus.ACTIVE ||
                status == VehicleStatus.IN_ROUTE ||
                status == VehicleStatus.AT_DEPOT;
    }

    private boolean matchesVehicleTypeFilter(Vehicle vehicle, FindAvailableVehiclesQuery query) {
        if (query.vehicleType().isEmpty()) {
            return true;
        }
        return vehicle.getVehicleType() == query.vehicleType().get();
    }

    private boolean matchesCapacityFilter(Vehicle vehicle, FindAvailableVehiclesQuery query) {
        if (query.minimumCapacity().isEmpty()) {
            return true;
        }

        return vehicle.getCapacity() != null &&
                vehicle.getCapacity().getTotalCapacity() >= query.minimumCapacity().get();
    }

    private boolean matchesLocationFilter(Vehicle vehicle, FindAvailableVehiclesQuery query) {
        if (query.nearLocation().isEmpty()) {
            return true;
        }

        return isVehicleNearLocation(vehicle, query);
    }

    private boolean matchesGpsFilter(Vehicle vehicle, FindAvailableVehiclesQuery query) {
        if (!query.requireRecentGps()) {
            return true;
        }

        return vehicle.hasRecentGpsData();
    }

    private boolean isVehicleNearLocation(Vehicle vehicle, FindAvailableVehiclesQuery query) {
        GeoCoordinate vehicleLocation = vehicle.getCurrentLocation();
        Optional<GeoCoordinate> searchLocation = query.nearLocation();

        if (vehicleLocation == null || searchLocation.isEmpty()) {
            return false;
        }

        try {
            double distanceKm = vehicleLocation.distanceTo(searchLocation.get()) / 1000.0;
            return distanceKm <= query.maxDistanceKm();
        } catch (Exception e) {
            log.warn("Failed to calculate distance for vehicle {}: {}",
                    vehicle.getId(), e.getMessage());
            return false;
        }
    }

    private PageResult<Vehicle> applyPagination(List<Vehicle> vehicles,
                                                tm.ugur.ugur_v3.application.shared.pagination.PageRequest pageRequest) {
        int totalElements = vehicles.size();
        long startIndex = pageRequest.getOffset();
        int endIndex = (int) Math.min(startIndex + pageRequest.getPageSize(), totalElements);

        if (startIndex >= totalElements) {
            return PageResult.of(List.of(), pageRequest, totalElements);
        }

        List<Vehicle> pageContent = vehicles.subList((int) startIndex, endIndex);
        return PageResult.of(pageContent, pageRequest, totalElements);
    }

    private void cacheSearchResult(FindAvailableVehiclesQuery query, FindAvailableVehiclesResult result) {
        try {
            String cacheKeyStr = buildCacheKey(query);
            CacheKey cacheKey = CacheKey.of("vehicle", "search", cacheKeyStr);


            Duration cacheTtl = determineCacheTtl(query);

            cacheManager.put(cacheKey, result, cacheTtl);
            log.debug("Cached available vehicles search result for {} minutes",
                    cacheTtl.toMinutes());

        } catch (Exception e) {
            log.warn("Failed to cache search result: {}", e.getMessage());

        }
    }

    private Duration determineCacheTtl(FindAvailableVehiclesQuery query) {
        if (query.requireRecentGps()) {
            return Duration.ofMinutes(1);
        } else if (query.hasFiltersApplied()) {
            return Duration.ofMinutes(3);
        } else {
            return Duration.ofMinutes(5);
        }
    }

    private String buildCacheKey(FindAvailableVehiclesQuery query) {
        StringBuilder key = new StringBuilder("vehicles:available");

        query.vehicleType().ifPresent(type -> key.append(":type=").append(type.name()));
        query.minimumCapacity().ifPresent(cap -> key.append(":cap=").append(cap));

        if (query.requireRecentGps()) {
            key.append(":gps");
        }

        if (query.nearLocation().isPresent()) {
            GeoCoordinate location = query.nearLocation().get();
            key.append(String.format(":loc=%.3f,%.3f:dist=%.1f",
                    location.getLatitude(),
                    location.getLongitude(),
                    query.maxDistanceKm()));
        }

        key.append(":page=").append(query.pageRequest().getPageNumber());
        key.append(":size=").append(query.pageRequest().getPageSize());

        return key.toString();
    }

    private void logSearchResult(FindAvailableVehiclesQuery query, FindAvailableVehiclesResult result) {
        log.debug("Available vehicles search completed: {} (cache: {}, time: {}ms)",
                result.getSearchSummary(),
                result.fromCache() ? "HIT" : "MISS",
                result.queryTime().toMillis());
    }

    private Mono<FindAvailableVehiclesResult> handleSearchError(FindAvailableVehiclesQuery query,
                                                                Throwable throwable,
                                                                Instant startTime) {
        Duration queryTime = Duration.between(startTime, Instant.now());

        log.error("Available vehicles search failed: {}", throwable.getMessage(), throwable);
        performanceMonitor.incrementCounter("vehicle.search.error");

        return Mono.just(FindAvailableVehiclesResult.empty(queryTime, query.pageRequest()));
    }
}