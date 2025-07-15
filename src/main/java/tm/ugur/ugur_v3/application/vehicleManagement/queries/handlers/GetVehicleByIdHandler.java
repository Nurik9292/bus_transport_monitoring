package tm.ugur.ugur_v3.application.vehicleManagement.queries.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.application.shared.caching.CacheKey;
import tm.ugur.ugur_v3.application.shared.caching.CacheManager;
import tm.ugur.ugur_v3.application.shared.caching.CacheStrategy;
import tm.ugur.ugur_v3.application.shared.executor.UseCaseExecutor;
import tm.ugur.ugur_v3.application.shared.monitoring.PerformanceMonitor;
import tm.ugur.ugur_v3.application.shared.validation.ValidatorService;
import tm.ugur.ugur_v3.application.vehicleManagement.queries.GetVehicleByIdQuery;
import tm.ugur.ugur_v3.application.vehicleManagement.queries.results.GetVehicleByIdResult;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;
import tm.ugur.ugur_v3.domain.vehicleManagement.repository.VehicleRepository;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetVehicleByIdHandler implements UseCaseExecutor.QueryHandler<GetVehicleByIdQuery, Mono<GetVehicleByIdResult>> {

    private final VehicleRepository vehicleRepository;
    private final CacheManager cacheManager;
    private final PerformanceMonitor performanceMonitor;
    private final ValidatorService validatorService;

    @Override
    public Mono<GetVehicleByIdResult> handle(GetVehicleByIdQuery query) {
        return performanceMonitor.timeReactive("vehicle.get.by.id", () ->
                processQuery(query)
        );
    }

    private Mono<GetVehicleByIdResult> processQuery(GetVehicleByIdQuery query) {
        return validateQuery(query)
                .then(tryFromCacheIfEnabled(query))
                .switchIfEmpty(fetchFromRepository(query))
                .doOnSuccess(result -> logQueryResult(query, result))
                .onErrorResume(throwable -> {
                    log.error("Failed to get vehicle by ID {}: {}",
                            query.vehicleId(), throwable.getMessage());
                    return Mono.just(GetVehicleByIdResult.notFound());
                });
    }

    private Mono<Void> validateQuery(GetVehicleByIdQuery query) {
        return Mono.fromRunnable(() -> {
            try {
                validatorService.validate(query);
                log.debug("Query validation passed for vehicle ID: {}", query.vehicleId());
            } catch (Exception e) {
                log.warn("Query validation failed for vehicle ID {}: {}", query.vehicleId(), e.getMessage());
                throw new IllegalArgumentException("Query validation failed: " + e.getMessage(), e);
            }
        });
    }

    private Mono<GetVehicleByIdResult> tryFromCacheIfEnabled(GetVehicleByIdQuery query) {
        if (!query.useCache()) {
            return Mono.empty();
        }

        String cacheKeyStr = buildCacheKey(query);
        CacheKey cacheKey = CacheKey.of("vehicle", "data", cacheKeyStr);

        return Mono.fromCallable(() ->
                        cacheManager.get(cacheKey, () -> null, Duration.ofMinutes(5))
                )
                .cast(Vehicle.class)
                .map(cachedVehicle -> {
                    log.debug("Vehicle {} found in cache", query.vehicleId());
                    performanceMonitor.incrementCounter("vehicle.get.cache.hit");

                    return query.includeLocationHistory() ?
                            GetVehicleByIdResult.foundWithHistory(cachedVehicle, true, Duration.ofMillis(1)) :
                            GetVehicleByIdResult.found(cachedVehicle, true, Duration.ofMillis(1));
                })
                .doOnError(error -> {
                    log.warn("Cache lookup failed for vehicle {}: {}", query.vehicleId(), error.getMessage());
                    performanceMonitor.incrementCounter("vehicle.get.cache.error");
                })
                .onErrorResume(throwable -> Mono.empty());
    }

    private Mono<GetVehicleByIdResult> fetchFromRepository(GetVehicleByIdQuery query) {
        java.time.Instant startTime = java.time.Instant.now();

        return vehicleRepository.findById(query.vehicleId())
                .map(vehicle -> {
                    java.time.Duration queryTime = java.time.Duration.between(startTime, java.time.Instant.now());

                    if (query.useCache()) {
                        cacheVehicle(query, vehicle);
                    }

                    performanceMonitor.incrementCounter("vehicle.get.repository.hit");
                    log.debug("Vehicle {} found in repository in {}ms",
                            query.vehicleId(), queryTime.toMillis());

                    return query.includeLocationHistory() ?
                            GetVehicleByIdResult.foundWithHistory(vehicle, false, queryTime) :
                            GetVehicleByIdResult.found(vehicle, false, queryTime);
                })
                .defaultIfEmpty(GetVehicleByIdResult.notFound())
                .doOnNext(result -> {
                    if (!result.isFound()) {
                        performanceMonitor.incrementCounter("vehicle.get.not.found");
                        log.debug("Vehicle {} not found", query.vehicleId());
                    }
                });
    }

    private void cacheVehicle(GetVehicleByIdQuery query, Vehicle vehicle) {
        try {
            String cacheKey = buildCacheKey(query);
            CacheStrategy.CacheStrategyConfig strategy = determineCacheStrategy(vehicle, query);

            CacheKey fullCacheKey = CacheKey.of("vehicle", "data", cacheKey);
            cacheManager.put(fullCacheKey, vehicle, strategy.getTtl());

            log.debug("Cached vehicle {} with strategy {}", query.vehicleId(), strategy);

        } catch (Exception e) {
            log.warn("Failed to cache vehicle {}: {}", query.vehicleId(), e.getMessage());
        }
    }

    private CacheStrategy.CacheStrategyConfig determineCacheStrategy(Vehicle vehicle, GetVehicleByIdQuery query) {
        if (vehicle.getStatus() == VehicleStatus.IN_ROUTE) {
            return CacheStrategy.SHORT_TTL;
        }

        if (query.includeLocationHistory()) {
            return CacheStrategy.MEDIUM_TTL;
        }

        return CacheStrategy.LONG_TTL;
    }

    private String buildCacheKey(GetVehicleByIdQuery query) {
        String baseKey = "vehicle:id:" + query.vehicleId().getValue();

        if (query.includeLocationHistory()) {
            baseKey += ":history";
        }

        return baseKey;
    }

    private void logQueryResult(GetVehicleByIdQuery query, GetVehicleByIdResult result) {
        if (result.isFound()) {
            log.debug("Successfully retrieved vehicle {}: {} (cache: {}, time: {}ms)",
                    query.vehicleId(),
                    result.getVehicleSummary(),
                    result.fromCache() ? "HIT" : "MISS",
                    result.queryTime().toMillis());
        } else {
            log.debug("Vehicle {} not found", query.vehicleId());
        }
    }
}