package tm.ugur.ugur_v3.application.vehicleManagement.queries.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.application.shared.caching.CacheKey;
import tm.ugur.ugur_v3.application.shared.caching.CacheManager;
import tm.ugur.ugur_v3.application.shared.caching.CacheStrategy;
import tm.ugur.ugur_v3.application.shared.executor.UseCaseExecutor;
import tm.ugur.ugur_v3.application.shared.monitoring.PerformanceMonitor;
import tm.ugur.ugur_v3.application.shared.validation.ValidatorService;
import tm.ugur.ugur_v3.application.vehicleManagement.queries.FindVehicleByLicensePlateQuery;
import tm.ugur.ugur_v3.application.vehicleManagement.queries.results.FindVehicleByLicensePlateResult;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.repository.VehicleRepository;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.LicensePlate;

import java.time.Duration;
import java.time.Instant;

@Log4j2
@Component
@RequiredArgsConstructor
public class FindVehicleByLicensePlateHandler implements UseCaseExecutor.QueryHandler<FindVehicleByLicensePlateQuery, Mono<FindVehicleByLicensePlateResult>> {

    private final VehicleRepository vehicleRepository;
    private final CacheManager cacheManager;
    private final PerformanceMonitor performanceMonitor;
    private final ValidatorService validatorService;

    @Override
    public Mono<FindVehicleByLicensePlateResult> handle(FindVehicleByLicensePlateQuery query) {
        return performanceMonitor.timeReactive("vehicle.find.by.license.plate", () ->
                processQuery(query)
        );
    }

    private Mono<FindVehicleByLicensePlateResult> processQuery(FindVehicleByLicensePlateQuery query) {
        Instant startTime = Instant.now();

        return validateQuery(query)
                .then(tryFromCacheIfEnabled(query))
                .switchIfEmpty(fetchFromRepository(query))
                .doOnNext(result -> cacheResultIfFound(query, result))
                .doOnNext(result -> logQueryResult(query, result, startTime))
                .onErrorResume(throwable -> handleQueryError(query, throwable, startTime));
    }

    private Mono<Void> validateQuery(FindVehicleByLicensePlateQuery query) {
        return Mono.fromRunnable(() -> validatorService.validate(query));
    }

    private Mono<FindVehicleByLicensePlateResult> tryFromCacheIfEnabled(FindVehicleByLicensePlateQuery query) {
        if (!query.isCacheable()) {
            return Mono.empty();
        }

        return Mono.fromCallable(() -> {
                    CacheKey cacheKey = CacheKey.Vehicle.metadata(query.licensePlate());
                    return cacheManager.get(cacheKey,
                            () -> null,
                            CacheStrategy.VEHICLE_LOCATION.getTtl());
                })
                .cast(Vehicle.class)
                .map(vehicle -> FindVehicleByLicensePlateResult.found(vehicle, true, Duration.ofMillis(1)))
                .doOnNext(result -> {
                    performanceMonitor.incrementCounter("vehicle.find.license.cache.hit");
                    log.trace("Cache HIT for license plate: {}", query.licensePlate());
                })
                .onErrorResume(error -> {
                    log.debug("Cache access failed for license plate {}: {}", query.licensePlate(), error.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<FindVehicleByLicensePlateResult> fetchFromRepository(FindVehicleByLicensePlateQuery query) {
        return Mono.fromCallable(() -> {
                    try {
                        return LicensePlate.of(query.licensePlate());
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid license plate format: " + query.licensePlate(), e);
                    }
                })
                .flatMap(licensePlate ->
                        vehicleRepository.findByLicensePlate(licensePlate)
                                .map(vehicle -> {
                                    Duration queryTime = Duration.between(Instant.now().minusMillis(1), Instant.now());
                                    return FindVehicleByLicensePlateResult.found(vehicle, false, queryTime);
                                })
                                .defaultIfEmpty(FindVehicleByLicensePlateResult.notFound(LicensePlate.of(query.licensePlate())))
                )
                .doOnNext(result -> {
                    if (result.isFound()) {
                        performanceMonitor.incrementCounter("vehicle.find.license.repository.hit");
                    } else {
                        performanceMonitor.incrementCounter("vehicle.find.license.repository.miss");
                    }
                });
    }

    private void cacheResultIfFound(FindVehicleByLicensePlateQuery query, FindVehicleByLicensePlateResult result) {
        if (result.isFound() && query.isCacheable() && !result.fromCache()) {
            try {
                CacheKey cacheKey = CacheKey.Vehicle.metadata(query.licensePlate());
                cacheManager.putAsync(cacheKey, result.getVehicleOptional().orElse(null),
                        CacheStrategy.VEHICLE_LOCATION.getTtl());

                log.trace("Cached vehicle for license plate: {}", query.licensePlate());
            } catch (Exception e) {
                log.warn("Failed to cache vehicle for license plate {}: {}", query.licensePlate(), e.getMessage());
            }
        }
    }

    private void logQueryResult(FindVehicleByLicensePlateQuery query, FindVehicleByLicensePlateResult result, Instant startTime) {
        Duration totalTime = Duration.between(startTime, Instant.now());

        if (result.isFound()) {
            log.debug("Found vehicle by license plate {}: {} (cache: {}, time: {}ms)",
                    query.licensePlate(),
                    result.getVehicleOptional().map(v -> v.getId().getValue()).orElse("unknown"),
                    result.fromCache() ? "HIT" : "MISS",
                    totalTime.toMillis());
        } else {
            log.debug("Vehicle not found for license plate: {} (time: {}ms)",
                    query.licensePlate(), totalTime.toMillis());
        }
    }

    private Mono<FindVehicleByLicensePlateResult> handleQueryError(FindVehicleByLicensePlateQuery query,
                                                                   Throwable throwable, Instant startTime) {

        Duration totalTime = Duration.between(startTime, Instant.now());
        performanceMonitor.incrementCounter("vehicle.find.license.error");

        String errorMessage = throwable.getMessage();
        if (errorMessage == null) {
            errorMessage = "Unknown error occurred during license plate search";
        }

        log.error("Error finding vehicle by license plate {}: {}", query.licensePlate(), errorMessage, throwable);

        return Mono.just(FindVehicleByLicensePlateResult.error(LicensePlate.of(query.licensePlate()), errorMessage, totalTime));
    }
}
