package tm.ugur.ugur_v3.application.vehicleManagement.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.application.shared.executor.UseCaseExecutor;
import tm.ugur.ugur_v3.application.shared.monitoring.PerformanceMonitor;
import tm.ugur.ugur_v3.application.shared.pagination.PageRequest;
import tm.ugur.ugur_v3.application.vehicleManagement.commands.ChangeVehicleStatusCommand;
import tm.ugur.ugur_v3.application.vehicleManagement.commands.UpdateVehicleLocationCommand;
import tm.ugur.ugur_v3.application.vehicleManagement.commands.handlers.ChangeVehicleStatusHandler;
import tm.ugur.ugur_v3.application.vehicleManagement.commands.handlers.UpdateVehicleLocationHandler;
import tm.ugur.ugur_v3.application.vehicleManagement.commands.results.ChangeVehicleStatusResult;
import tm.ugur.ugur_v3.application.vehicleManagement.commands.results.UpdateVehicleLocationResult;
import tm.ugur.ugur_v3.application.vehicleManagement.queries.FindAvailableVehiclesQuery;
import tm.ugur.ugur_v3.application.vehicleManagement.queries.GetVehicleByIdQuery;
import tm.ugur.ugur_v3.application.vehicleManagement.queries.handlers.FindAvailableVehiclesHandler;
import tm.ugur.ugur_v3.application.vehicleManagement.queries.handlers.GetVehicleByIdHandler;
import tm.ugur.ugur_v3.application.vehicleManagement.queries.results.FindAvailableVehiclesResult;
import tm.ugur.ugur_v3.application.vehicleManagement.queries.results.GetVehicleByIdResult;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleType;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleApplicationService {

    private final UseCaseExecutor useCaseExecutor;
    private final PerformanceMonitor performanceMonitor;

    private final UpdateVehicleLocationHandler updateLocationHandler;
    private final ChangeVehicleStatusHandler changeStatusHandler;

    private final GetVehicleByIdHandler getByIdHandler;
    private final FindAvailableVehiclesHandler findAvailableHandler;


    public Mono<UpdateVehicleLocationResult> updateVehicleLocation(VehicleId vehicleId,
                                                                   GeoCoordinate location,
                                                                   Double speedKmh,
                                                                   Double bearingDegrees) {
        UpdateVehicleLocationCommand command = UpdateVehicleLocationCommand.create(
                vehicleId, location, speedKmh, bearingDegrees);

        return updateLocationHandler.handle(command)
                .doOnSuccess(result -> {
                    if (result.isSuccess()) {
                        log.debug("Vehicle location updated successfully: {}", vehicleId);
                        performanceMonitor.incrementCounter("vehicle.location.updated.success");
                    } else {
                        log.warn("Vehicle location update failed: {} - {}", vehicleId, result.getErrorMessage());
                        performanceMonitor.incrementCounter("vehicle.location.updated.failure");
                    }
                })
                .doOnError(error -> {
                    log.error("Vehicle location update error for {}: {}", vehicleId, error.getMessage());
                    performanceMonitor.incrementCounter("vehicle.location.updated.error");
                });
    }

    public Mono<UpdateVehicleLocationResult> updateVehicleLocationFromGps(VehicleId vehicleId,
                                                                          double latitude,
                                                                          double longitude,
                                                                          double accuracy,
                                                                          Double speedKmh,
                                                                          Double bearingDegrees) {
        UpdateVehicleLocationCommand command = UpdateVehicleLocationCommand.fromGpsApi(
                vehicleId, latitude, longitude, accuracy, speedKmh, bearingDegrees);

        return updateLocationHandler.handle(command);
    }

    public Flux<UpdateVehicleLocationResult> updateVehicleLocationsBatch(Flux<UpdateVehicleLocationCommand> commands) {
        return commands
                .flatMap(updateLocationHandler::handle, 20) // Controlled concurrency
                .doOnNext(result -> {
                    if (result.isSuccess()) {
                        performanceMonitor.incrementCounter("vehicle.location.batch.success");
                    } else {
                        performanceMonitor.incrementCounter("vehicle.location.batch.failure");
                    }
                })
                .doOnError(error -> {
                    log.error("Batch location update error: {}", error.getMessage());
                    performanceMonitor.incrementCounter("vehicle.location.batch.error");
                });
    }

    public Mono<ChangeVehicleStatusResult> changeVehicleStatus(VehicleId vehicleId,
                                                               VehicleStatus newStatus,
                                                               String reason,
                                                               String changedBy) {
        ChangeVehicleStatusCommand command = ChangeVehicleStatusCommand.create(
                vehicleId, newStatus, reason, changedBy);

        return changeStatusHandler.handle(command)
                .doOnSuccess(result -> {
                    if (result.isSuccess()) {
                        log.info("Vehicle status changed: {} to {}", vehicleId, newStatus);
                        performanceMonitor.incrementCounter("vehicle.status.changed.success");
                    } else {
                        log.warn("Vehicle status change failed: {} - {}", vehicleId, result.getErrorMessage());
                        performanceMonitor.incrementCounter("vehicle.status.changed.failure");
                    }
                })
                .doOnError(error -> {
                    log.error("Vehicle status change error for {}: {}", vehicleId, error.getMessage());
                    performanceMonitor.incrementCounter("vehicle.status.changed.error");
                });
    }

    public Mono<ChangeVehicleStatusResult> changeVehicleStatusAutomatic(VehicleId vehicleId,
                                                                        VehicleStatus newStatus,
                                                                        String reason) {
        ChangeVehicleStatusCommand command = ChangeVehicleStatusCommand.automatic(
                vehicleId, newStatus, reason);

        return changeStatusHandler.handle(command);
    }


    public Mono<GetVehicleByIdResult> getVehicleById(VehicleId vehicleId) {
        GetVehicleByIdQuery query = GetVehicleByIdQuery.simple(vehicleId);

        return getByIdHandler.handle(query)
                .doOnSuccess(result -> {
                    if (result.isFound()) {
                        performanceMonitor.incrementCounter("vehicle.get.success");
                        log.debug("Vehicle found: {}", result.getVehicleSummary());
                    } else {
                        performanceMonitor.incrementCounter("vehicle.get.not.found");
                        log.debug("Vehicle not found: {}", vehicleId);
                    }
                });
    }

    public Mono<GetVehicleByIdResult> getVehicleByIdWithHistory(VehicleId vehicleId) {
        GetVehicleByIdQuery query = GetVehicleByIdQuery.withHistory(vehicleId);
        return getByIdHandler.handle(query);
    }

    public Mono<GetVehicleByIdResult> getVehicleByIdRealTime(VehicleId vehicleId) {
        GetVehicleByIdQuery query = GetVehicleByIdQuery.realTime(vehicleId);
        return getByIdHandler.handle(query);
    }

    public Mono<FindAvailableVehiclesResult> findAvailableVehicles(PageRequest pageRequest) {
        FindAvailableVehiclesQuery query = FindAvailableVehiclesQuery.all(pageRequest);

        return findAvailableHandler.handle(query)
                .doOnSuccess(result -> {
                    performanceMonitor.incrementCounter("vehicle.search.success");
                    log.debug("Found available vehicles: {}", result.getSearchSummary());
                });
    }

    public Mono<FindAvailableVehiclesResult> findAvailableVehiclesByType(VehicleType vehicleType,
                                                                         PageRequest pageRequest) {
        FindAvailableVehiclesQuery query = FindAvailableVehiclesQuery.byType(pageRequest, vehicleType);
        return findAvailableHandler.handle(query);
    }

    public Mono<FindAvailableVehiclesResult> findAvailableVehiclesNearLocation(GeoCoordinate location,
                                                                               double radiusKm,
                                                                               PageRequest pageRequest) {
        FindAvailableVehiclesQuery query = FindAvailableVehiclesQuery.nearLocation(
                pageRequest, location, radiusKm);
        return findAvailableHandler.handle(query);
    }

    public Mono<FindAvailableVehiclesResult> findAvailableVehiclesWithCapacity(int minimumCapacity,
                                                                               PageRequest pageRequest) {
        FindAvailableVehiclesQuery query = FindAvailableVehiclesQuery.withMinimumCapacity(
                pageRequest, minimumCapacity);
        return findAvailableHandler.handle(query);
    }


    public Mono<Boolean> isVehicleAvailable(VehicleId vehicleId) {
        return getVehicleById(vehicleId)
                .map(result -> result.isFound() &&
                        (result.vehicle().getStatus() == VehicleStatus.ACTIVE ||
                                result.vehicle().getStatus() == VehicleStatus.IN_ROUTE))
                .onErrorReturn(false);
    }

    public Mono<GeoCoordinate> getCurrentVehicleLocation(VehicleId vehicleId) {
        return getVehicleById(vehicleId)
                .mapNotNull(result -> result.isFound() ? result.vehicle().getCurrentLocation() : null);
    }

    public Mono<VehicleStatus> getVehicleStatus(VehicleId vehicleId) {
        return getVehicleById(vehicleId)
                .mapNotNull(result -> result.isFound() ? result.vehicle().getStatus() : null);
    }

    // ============= BATCH OPERATIONS =============

    public Flux<ChangeVehicleStatusResult> changeVehicleStatusBatch(Flux<ChangeVehicleStatusCommand> commands) {
        return commands
                .flatMap(changeStatusHandler::handle, 10) // Controlled concurrency for status changes
                .doOnNext(result -> {
                    if (result.isSuccess()) {
                        performanceMonitor.incrementCounter("vehicle.status.batch.success");
                    } else {
                        performanceMonitor.incrementCounter("vehicle.status.batch.failure");
                    }
                });
    }

    public Flux<GetVehicleByIdResult> getVehiclesByIds(Flux<VehicleId> vehicleIds) {
        return vehicleIds
                .flatMap(this::getVehicleById, 15) // Controlled concurrency
                .doOnNext(result -> {
                    if (result.isFound()) {
                        performanceMonitor.incrementCounter("vehicle.batch.get.success");
                    } else {
                        performanceMonitor.incrementCounter("vehicle.batch.get.not.found");
                    }
                });
    }

    // ============= SYSTEM HEALTH AND MONITORING =============

    public Mono<VehicleSystemHealth> getSystemHealth() {
        return Mono.fromCallable(() -> {
            long totalOperations = performanceMonitor.getCounterValue("vehicle.operations.total");
            long successfulOperations = performanceMonitor.getCounterValue("vehicle.location.updated.success") +
                    performanceMonitor.getCounterValue("vehicle.status.changed.success") +
                    performanceMonitor.getCounterValue("vehicle.get.success");
            long errorOperations = performanceMonitor.getCounterValue("vehicle.location.updated.error") +
                    performanceMonitor.getCounterValue("vehicle.status.changed.error");

            double avgResponseTime = performanceMonitor.getTimerMean("vehicle.operations.time");
            double errorRate = totalOperations > 0 ? (double) errorOperations / totalOperations * 100.0 : 0.0;

            boolean isHealthy = avgResponseTime < 100 && errorRate < 1.0;

            return new VehicleSystemHealth(
                    successfulOperations,
                    errorOperations,
                    avgResponseTime,
                    errorRate,
                    isHealthy,
                    java.time.Instant.now()
            );
        });
    }

    public Mono<VehiclePerformanceMetrics> getPerformanceMetrics() {
        return Mono.fromCallable(() -> {
            double locationUpdateTime = performanceMonitor.getTimerMean("vehicle.location.update");
            double statusChangeTime = performanceMonitor.getTimerMean("vehicle.status.change");
            double queryTime = performanceMonitor.getTimerMean("vehicle.get.by.id");
            double searchTime = performanceMonitor.getTimerMean("vehicle.find.available");

            long cacheHits = performanceMonitor.getCounterValue("vehicle.get.cache.hit") +
                    performanceMonitor.getCounterValue("vehicle.search.cache.hit");
            long cacheMisses = performanceMonitor.getCounterValue("vehicle.get.repository.hit") +
                    performanceMonitor.getCounterValue("vehicle.search.repository.hit");

            double cacheHitRate = (cacheHits + cacheMisses) > 0 ?
                    (double) cacheHits / (cacheHits + cacheMisses) * 100.0 : 0.0;

            return new VehiclePerformanceMetrics(
                    locationUpdateTime,
                    statusChangeTime,
                    queryTime,
                    searchTime,
                    cacheHitRate,
                    java.time.Instant.now()
            );
        });
    }


    public record VehicleSystemHealth(
            long successfulOperations,
            long errorOperations,
            double averageResponseTimeMs,
            double errorRatePercentage,
            boolean isSystemHealthy,
            java.time.Instant lastCheck
    ) {
        public long getTotalOperations() {
            return successfulOperations + errorOperations;
        }

        public double getSuccessRate() {
            long total = getTotalOperations();
            return total > 0 ? (double) successfulOperations / total * 100.0 : 0.0;
        }
    }

    public record VehiclePerformanceMetrics(
            double avgLocationUpdateTimeMs,
            double avgStatusChangeTimeMs,
            double avgQueryTimeMs,
            double avgSearchTimeMs,
            double cacheHitRatePercentage,
            java.time.Instant lastUpdate
    ) {
        public boolean areOperationsPerformant() {
            return avgLocationUpdateTimeMs < 100 &&
                    avgStatusChangeTimeMs < 200 &&
                    avgQueryTimeMs < 50 &&
                    avgSearchTimeMs < 150;
        }

        public boolean isCacheEffective() {
            return cacheHitRatePercentage > 60.0; // At least 60% cache hit rate
        }
    }
}