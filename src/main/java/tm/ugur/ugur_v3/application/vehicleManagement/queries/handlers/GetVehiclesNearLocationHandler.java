/**
 * COMPONENT: GetVehiclesNearLocationHandler - Fixed Reactive Implementation
 * LAYER: Application/VehicleManagement/Queries/Handlers
 * PURPOSE: Handler для поиска транспортных средств рядом с местоположением
 * PERFORMANCE TARGET: < 150ms geospatial query processing
 * SCALABILITY: Non-blocking reactive обработка для geo queries
 */
package tm.ugur.ugur_v3.application.vehicleManagement.queries.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.application.shared.executor.UseCaseExecutor;
import tm.ugur.ugur_v3.application.shared.monitoring.PerformanceMonitor;
import tm.ugur.ugur_v3.application.shared.validation.ValidatorService;
import tm.ugur.ugur_v3.application.vehicleManagement.queries.GetVehiclesNearLocationQuery;
import tm.ugur.ugur_v3.application.vehicleManagement.queries.results.GetVehiclesNearLocationResult;
import tm.ugur.ugur_v3.application.vehicleManagement.queries.results.VehicleNearLocationResult;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;
import tm.ugur.ugur_v3.domain.vehicleManagement.repository.VehicleRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetVehiclesNearLocationHandler implements UseCaseExecutor.QueryHandler<GetVehiclesNearLocationQuery, Mono<GetVehiclesNearLocationResult>> {

    private final VehicleRepository vehicleRepository;
    private final PerformanceMonitor performanceMonitor;
    private final ValidatorService validatorService;

    @Override
    public Mono<GetVehiclesNearLocationResult> handle(GetVehiclesNearLocationQuery query) {
        return performanceMonitor.timeReactive("vehicle.find.near.location", () ->
                processNearLocationSearch(query)
        );
    }

    private Mono<GetVehiclesNearLocationResult> processNearLocationSearch(GetVehiclesNearLocationQuery query) {
        Instant startTime = Instant.now();

        return validateQuery(query)
                .then(searchNearbyVehicles(query))
                .map(results -> createSuccessResult(query, results, startTime))
                .doOnSuccess(result -> logSearchResult(query, result))
                .onErrorResume(throwable -> handleSearchError(query, throwable, startTime));
    }

    private Mono<Void> validateQuery(GetVehiclesNearLocationQuery query) {
        return Mono.fromRunnable(() -> {
            try {
                validatorService.validate(query);

                if (query.radiusKm() <= 0 || query.radiusKm() > 100) {
                    throw new IllegalArgumentException("Search radius must be between 0 and 100 km");
                }

                if (query.limit() <= 0 || query.limit() > 1000) {
                    throw new IllegalArgumentException("Result limit must be between 1 and 1000");
                }

                log.debug("Near location search query validation passed");
            } catch (Exception e) {
                log.warn("Near location search query validation failed: {}", e.getMessage());
                throw new IllegalArgumentException("Query validation failed: " + e.getMessage(), e);
            }
        });
    }

    private Mono<List<VehicleNearLocationResult>> searchNearbyVehicles(GetVehiclesNearLocationQuery query) {
        double radiusMeters = query.radiusKm() * 1000.0;

        return vehicleRepository.findWithinRadius(query.location(), radiusMeters)
                .filter(vehicle -> matchesStatusFilter(vehicle, query))
                .filter(vehicle -> matchesTypeFilter(vehicle, query))
                .filter(vehicle -> matchesGpsFilter(vehicle, query))
                .filter(this::hasValidLocation)
                .map(vehicle -> createVehicleResult(vehicle, query))
                .collectList()
                .map(results -> results.stream()
                        .sorted(Comparator.comparingDouble(VehicleNearLocationResult::distanceMeters))
                        .limit(query.limit())
                        .toList()
                );
    }

    private boolean matchesStatusFilter(Vehicle vehicle, GetVehiclesNearLocationQuery query) {
        if (!query.includeInactive()) {
            return vehicle.getStatus() == VehicleStatus.ACTIVE ||
                    vehicle.getStatus() == VehicleStatus.IN_ROUTE ||
                    vehicle.getStatus() == VehicleStatus.AT_DEPOT;
        }


        return vehicle.getStatus() != VehicleStatus.RETIRED &&
                vehicle.getStatus() != VehicleStatus.INACTIVE;
    }

    private boolean matchesTypeFilter(Vehicle vehicle, GetVehiclesNearLocationQuery query) {
        if (query.vehicleType().isEmpty()) {
            return true;
        }
        return vehicle.getVehicleType() == query.vehicleType().get();
    }

    private boolean matchesGpsFilter(Vehicle vehicle, GetVehiclesNearLocationQuery query) {
        if (!query.requireRecentGps()) {
            return true;
        }
        return vehicle.hasRecentGpsData();
    }

    private boolean hasValidLocation(Vehicle vehicle) {
        return vehicle.getCurrentLocation() != null;
    }

    private VehicleNearLocationResult createVehicleResult(Vehicle vehicle, GetVehiclesNearLocationQuery query) {
        GeoCoordinate vehicleLocation = vehicle.getCurrentLocation();

        if (vehicleLocation == null) {
            log.warn("Vehicle {} has null location, cannot calculate distance", vehicle.getId());
            return VehicleNearLocationResult.withoutDistance(
                    vehicle.getId(),
                    vehicle,
                    vehicle.getCurrentSpeedKmh(),
                    vehicle.getStatus(),
                    vehicle.getLastLocationUpdate(),
                    vehicle.isMoving()
            );
        }

        if (query.includeDistance()) {
            return VehicleNearLocationResult.of(vehicle, query.location());
        } else {
            return VehicleNearLocationResult.withoutCalculatedDistance(
                    vehicle.getId(),
                    vehicle,
                    vehicle.getCurrentSpeedKmh(),
                    vehicle.getStatus(),
                    vehicleLocation,
                    vehicle.getLastLocationUpdate(),
                    vehicle.isMoving()
            );
        }
    }

    private GetVehiclesNearLocationResult createSuccessResult(GetVehiclesNearLocationQuery query,
                                                              List<VehicleNearLocationResult> results,
                                                              Instant startTime) {
        Duration queryTime = Duration.between(startTime, Instant.now());

        return GetVehiclesNearLocationResult.success(
                query.location(),
                query.radiusKm(),
                results,
                queryTime,
                query.includeDistance(),
                query.requireRecentGps()
        );
    }

    private void logSearchResult(GetVehiclesNearLocationQuery query, GetVehiclesNearLocationResult result) {
        log.debug("Near location search completed: found {} vehicles within {}km of ({}, {}) in {}ms",
                result.nearbyVehicles().size(),
                query.radiusKm(),
                query.location().getLatitude(),
                query.location().getLongitude(),
                result.queryTime().toMillis());
    }

    private Mono<GetVehiclesNearLocationResult> handleSearchError(GetVehiclesNearLocationQuery query,
                                                                  Throwable throwable,
                                                                  Instant startTime) {
        Duration queryTime = Duration.between(startTime, Instant.now());

        log.error("Near location search failed for location ({}, {}): {}",
                query.location().getLatitude(),
                query.location().getLongitude(),
                throwable.getMessage(), throwable);

        performanceMonitor.incrementCounter("vehicle.near.location.search.error");


        return Mono.just(GetVehiclesNearLocationResult.empty(
                query.location(),
                query.radiusKm(),
                queryTime,
                throwable.getMessage()
        ));
    }
}