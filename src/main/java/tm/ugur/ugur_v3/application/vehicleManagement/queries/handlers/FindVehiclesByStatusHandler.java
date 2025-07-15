package tm.ugur.ugur_v3.application.vehicleManagement.queries.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.application.shared.caching.CacheKey;
import tm.ugur.ugur_v3.application.shared.caching.CacheManager;
import tm.ugur.ugur_v3.application.shared.executor.UseCaseExecutor;
import tm.ugur.ugur_v3.application.shared.monitoring.PerformanceMonitor;
import tm.ugur.ugur_v3.application.shared.pagination.PageResult;
import tm.ugur.ugur_v3.application.shared.validation.ValidatorService;
import tm.ugur.ugur_v3.application.vehicleManagement.queries.FindVehiclesByStatusQuery;
import tm.ugur.ugur_v3.application.vehicleManagement.queries.results.FindVehiclesByStatusResult;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;
import tm.ugur.ugur_v3.domain.vehicleManagement.repository.VehicleRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Log4j2
@Component
@RequiredArgsConstructor
public class FindVehiclesByStatusHandler implements UseCaseExecutor.QueryHandler<FindVehiclesByStatusQuery, Mono<FindVehiclesByStatusResult>> {

    private final VehicleRepository vehicleRepository;
    private final CacheManager cacheManager;
    private final PerformanceMonitor performanceMonitor;
    private final ValidatorService validatorService;

    @Override
    public Mono<FindVehiclesByStatusResult> handle(FindVehiclesByStatusQuery query) {
        return performanceMonitor.timeReactive("vehicle.find.by.status", () ->
                processStatusSearch(query)
        );
    }

    private Mono<FindVehiclesByStatusResult> processStatusSearch(FindVehiclesByStatusQuery query) {
        Instant startTime = Instant.now();

        return validateQuery(query)
                .then(tryFromCacheIfEnabled(query, startTime))
                .switchIfEmpty(searchFromRepository(query, startTime))
                .doOnSuccess(result -> logSearchResult(query, result));
    }

    private Mono<Void> validateQuery(FindVehiclesByStatusQuery query) {
        return Mono.fromRunnable(() -> {
            try {
                validatorService.validate(query);
                log.debug("FindVehiclesByStatus query validation passed for status: {}", query.status());
            } catch (Exception e) {
                log.warn("FindVehiclesByStatus query validation failed: {}", e.getMessage());
                throw new IllegalArgumentException("Query validation failed: " + e.getMessage(), e);
            }
        });
    }

    private Mono<FindVehiclesByStatusResult> tryFromCacheIfEnabled(FindVehiclesByStatusQuery query,
                                                                   Instant startTime) {
        if (!isCacheableStatus(query.status())) {
            return Mono.empty();
        }

        String cacheKeyStr = buildCacheKey(query);
        CacheKey cacheKey = CacheKey.of("vehicle", "status", cacheKeyStr);

        return Mono.fromCallable(() ->
                        cacheManager.get(cacheKey, () -> null, Duration.ofMinutes(5))
                )
                .cast(FindVehiclesByStatusResult.class)
                .map(cachedResult -> {
                    Duration queryTime = Duration.between(startTime, Instant.now());
                    log.debug("Vehicles with status {} retrieved from cache", query.status());
                    performanceMonitor.incrementCounter("vehicle.status.search.cache.hit");


                    return FindVehiclesByStatusResult.success(
                            query.status(),
                            cachedResult.vehicles(),
                            true,
                            queryTime,
                            query.includeLocationData(),
                            query.requireRecentGps()
                    );
                })
                .doOnError(error -> {
                    log.warn("Cache lookup failed for status {}: {}", query.status(), error.getMessage());
                    performanceMonitor.incrementCounter("vehicle.status.search.cache.error");
                })
                .onErrorResume(throwable -> Mono.empty());
    }

    private Mono<FindVehiclesByStatusResult> searchFromRepository(FindVehiclesByStatusQuery query,
                                                                  Instant startTime) {
        return findVehiclesByStatus(query)
                .collectList()
                .map(vehicles -> {
                    Duration queryTime = Duration.between(startTime, Instant.now());
                    return createSuccessResult(query, vehicles, queryTime, false);
                })
                .doOnSuccess(result -> {
                    if (isCacheableStatus(query.status())) {
                        cacheSearchResult(query, result);
                    }
                    performanceMonitor.incrementCounter("vehicle.status.search.repository.hit");
                    log.debug("Found {} vehicles with status {} in {}ms",
                            result.vehicles().getTotalElements(),
                            query.status(),
                            result.queryTime().toMillis());
                });
    }

    private reactor.core.publisher.Flux<Vehicle> findVehiclesByStatus(FindVehiclesByStatusQuery query) {
        return vehicleRepository.findByStatus(query.status())
                .filter(vehicle -> matchesLocationFilter(vehicle, query))
                .filter(vehicle -> matchesGpsFilter(vehicle, query))
                .filter(vehicle -> matchesInactiveFilter(vehicle, query));
    }

    private boolean matchesLocationFilter(Vehicle vehicle, FindVehiclesByStatusQuery query) {
        if (!query.includeLocationData()) {
            return true;
        }


        return vehicle.getCurrentLocation() != null;
    }

    private boolean matchesGpsFilter(Vehicle vehicle, FindVehiclesByStatusQuery query) {
        if (!query.requireRecentGps()) {
            return true;
        }


        return vehicle.hasRecentGpsData();
    }

    private boolean matchesInactiveFilter(Vehicle vehicle, FindVehiclesByStatusQuery query) {
        if (query.includeInactiveVehicles()) {
            return true;
        }


        return vehicle.getStatus() != VehicleStatus.RETIRED &&
                vehicle.getStatus() != VehicleStatus.INACTIVE;
    }

    private FindVehiclesByStatusResult createSuccessResult(FindVehiclesByStatusQuery query,
                                                           List<Vehicle> vehicles,
                                                           Duration queryTime,
                                                           boolean fromCache) {

        PageResult<Vehicle> pageResult = applyPagination(vehicles, query.pageRequest());

        return FindVehiclesByStatusResult.success(
                query.status(),
                pageResult,
                fromCache,
                queryTime,
                query.includeLocationData(),
                query.requireRecentGps()
        );
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

    private boolean isCacheableStatus(VehicleStatus status) {

        return status == VehicleStatus.AT_DEPOT ||
                status == VehicleStatus.MAINTENANCE ||
                status == VehicleStatus.RETIRED ||
                status == VehicleStatus.INACTIVE;
    }

    private void cacheSearchResult(FindVehiclesByStatusQuery query, FindVehiclesByStatusResult result) {
        try {
            String cacheKeyStr = buildCacheKey(query);
            CacheKey cacheKey = CacheKey.of("vehicle", "status", cacheKeyStr);

            Duration cacheTtl = determineCacheTtl(query.status());
            cacheManager.put(cacheKey, result, cacheTtl);

            log.debug("Cached vehicles by status search for {} minutes", cacheTtl.toMinutes());
        } catch (Exception e) {
            log.warn("Failed to cache search result for status {}: {}", query.status(), e.getMessage());

        }
    }

    private Duration determineCacheTtl(VehicleStatus status) {
        return switch (status) {
            case ACTIVE, IN_ROUTE -> Duration.ofMinutes(2);
            case AT_DEPOT -> Duration.ofMinutes(5);
            case MAINTENANCE, RETIRED -> Duration.ofMinutes(15);
            default -> Duration.ofMinutes(3);
        };
    }

    private String buildCacheKey(FindVehiclesByStatusQuery query) {
        StringBuilder keyBuilder = new StringBuilder("vehicles:status=").append(query.status().name());

        if (query.includeLocationData()) {
            keyBuilder.append(":location");
        }
        if (query.requireRecentGps()) {
            keyBuilder.append(":gps");
        }
        if (query.includeInactiveVehicles()) {
            keyBuilder.append(":inactive");
        }

        keyBuilder.append(":page=").append(query.pageRequest().getPageNumber());
        keyBuilder.append(":size=").append(query.pageRequest().getPageSize());

        return keyBuilder.toString();
    }

    private void logSearchResult(FindVehiclesByStatusQuery query, FindVehiclesByStatusResult result) {
        log.debug("Vehicles by status search completed: status={}, found={}, cache={}, time={}ms",
                query.status().name(),
                result.vehicles().getTotalElements(),
                result.fromCache() ? "HIT" : "MISS",
                result.queryTime().toMillis());
    }
}