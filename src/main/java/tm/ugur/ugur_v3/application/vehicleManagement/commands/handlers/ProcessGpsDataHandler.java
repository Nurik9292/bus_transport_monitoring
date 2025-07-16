/**
 * ИСПРАВЛЕННЫЙ ProcessGpsDataHandler
 * ИСПОЛЬЗУЕТ РЕАЛЬНУЮ структуру VehicleManagementConfig
 */
package tm.ugur.ugur_v3.application.vehicleManagement.commands.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tm.ugur.ugur_v3.application.configuration.VehicleManagementConfig;
import tm.ugur.ugur_v3.application.shared.executor.UseCaseExecutor;
import tm.ugur.ugur_v3.application.shared.monitoring.PerformanceMonitor;
import tm.ugur.ugur_v3.application.vehicleManagement.commands.ProcessGpsDataCommand;
import tm.ugur.ugur_v3.application.vehicleManagement.commands.UpdateVehicleLocationCommand;
import tm.ugur.ugur_v3.application.vehicleManagement.commands.results.ProcessGpsDataResult;
import tm.ugur.ugur_v3.application.vehicleManagement.commands.handlers.UpdateVehicleLocationHandler;
import tm.ugur.ugur_v3.domain.vehicleManagement.services.GpsDataProvider;
import tm.ugur.ugur_v3.domain.vehicleManagement.repository.VehicleRepository;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.LicensePlate;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleType;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessGpsDataHandler implements UseCaseExecutor.CommandHandler<ProcessGpsDataCommand, ProcessGpsDataResult> {

    private final List<GpsDataProvider> gpsProviders;
    private final VehicleRepository vehicleRepository;
    private final VehicleManagementConfig config;
    private final PerformanceMonitor performanceMonitor;
    private final UpdateVehicleLocationHandler updateLocationHandler;

    private final Map<String, Timestamp> lastProcessedTimestamps = new ConcurrentHashMap<>();

    @Override
    public Mono<ProcessGpsDataResult> handle(ProcessGpsDataCommand command) {
        log.debug("Processing GPS data from {} providers", command.getProviderNames().size());

        return performanceMonitor.time("gps.data.processing",
                        processGpsDataFromProviders(command)
                )
                .doOnSuccess(result -> {
                    log.info("GPS processing completed: {} successful updates, {} errors",
                            result.getSuccessfulUpdates(), result.getErrorCount());
                    performanceMonitor.incrementCounter("gps.processing.completed");
                })
                .doOnError(error -> {
                    log.error("GPS processing failed", error);
                    performanceMonitor.incrementCounter("gps.processing.failed");
                });
    }

    private Mono<ProcessGpsDataResult> processGpsDataFromProviders(ProcessGpsDataCommand command) {
        List<GpsDataProvider> activeProviders = gpsProviders.stream()
                .filter(provider -> command.getProviderNames().contains(provider.getProviderName()))
                .toList();

        if (activeProviders.isEmpty()) {
            log.warn("No active GPS providers found for names: {}", command.getProviderNames());
            return Mono.just(ProcessGpsDataResult.noProviders(command.getProviderNames()));
        }

        return Flux.fromIterable(activeProviders)
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .flatMap(provider -> processProviderData(provider, command))
                .sequential()
                .collectList()
                .map(providerResults -> {
                    Duration processingTime = Duration.between(command.getRequestedAt(), Instant.now());
                    return ProcessGpsDataResult.fromProviderResults(providerResults, processingTime);
                });
    }

    private Flux<ProviderProcessingResult> processProviderData(GpsDataProvider provider, ProcessGpsDataCommand command) {
        String providerName = provider.getProviderName();

        // ✅ ИСПОЛЬЗУЕМ РЕАЛЬНЫЕ properties из VehicleManagementConfig
        Duration batchTimeout = config.getPerformance().getCommandTimeout();
        int batchSize = config.getPerformance().getBatchSize();

        return provider.getVehicleLocations()
                .timeout(batchTimeout)
                .buffer(batchSize)
                .flatMap(locations -> processBatch(locations, providerName, command))
                .onErrorResume(error -> {
                    log.error("Failed to process GPS data from provider: {}", providerName, error);
                    return Mono.just(ProviderProcessingResult.withError(providerName, error));
                });
    }

    private Mono<ProviderProcessingResult> processBatch(List<GpsDataProvider.GpsLocationData> locations,
                                                        String providerName,
                                                        ProcessGpsDataCommand command) {
        log.debug("Processing batch of {} locations from {}", locations.size(), providerName);

        int maxConcurrent = config.getPerformance().getMaxConcurrentLocationUpdates();

        return Flux.fromIterable(locations)
                .filter(this::isValidGpsData)
                .filter(this::isSignificantUpdate)
                .flatMap(gpsData -> updateVehicleLocation(gpsData, command), maxConcurrent)
                .collectList()
                .map(results -> ProviderProcessingResult.fromUpdates(results, providerName));
    }

    private Mono<LocationUpdateResult> updateVehicleLocation(GpsDataProvider.GpsLocationData gpsData,
                                                             ProcessGpsDataCommand command) {
        String vehicleIdentifier = gpsData.vehicleIdentifier();

        return findOrCreateVehicle(vehicleIdentifier)
                .flatMap(vehicle -> {
                    try {
                        UpdateVehicleLocationCommand locationCommand = UpdateVehicleLocationCommand.fromGpsApi(
                                vehicle.getId(),
                                gpsData.latitude(),
                                gpsData.longitude(),
                                gpsData.accuracy(),
                                gpsData.speed(),
                                gpsData.bearing()
                        );

                        // Используем существующий handler
                        return updateLocationHandler.handle(locationCommand)
                                .map(result -> LocationUpdateResult.successful(vehicleIdentifier, gpsData.timestamp()))
                                .onErrorResume(error -> {
                                    log.error("Failed to update vehicle location: {}", vehicleIdentifier, error);
                                    return Mono.just(LocationUpdateResult.failed(vehicleIdentifier, error));
                                });

                    } catch (Exception e) {
                        log.error("Failed to create location update command for vehicle: {}", vehicleIdentifier, e);
                        return Mono.just(LocationUpdateResult.failed(vehicleIdentifier, e));
                    }
                });
    }

    private Mono<Vehicle> findOrCreateVehicle(String vehicleIdentifier) {
        LicensePlate licensePlate = LicensePlate.of(vehicleIdentifier);

        return vehicleRepository.findByLicensePlate(licensePlate)
                .switchIfEmpty(createVehicleFromGpsData(vehicleIdentifier))
                .cache(config.getCache().getVehicleDataTtl());
    }

    private Mono<Vehicle> createVehicleFromGpsData(String vehicleIdentifier) {
        log.info("Creating new vehicle from GPS data: {}", vehicleIdentifier);

        return vehicleRepository.nextId()
                .map(vehicleId -> new Vehicle(
                        vehicleId,
                        vehicleIdentifier,
                        VehicleType.BUS,
                        50,
                        "Unknown Model"
                ))
                .flatMap(vehicleRepository::save);
    }

    private boolean isValidGpsData(GpsDataProvider.GpsLocationData gpsData) {
        var gpsValidation = config.getValidation().getGps();

        if (gpsData.accuracy() > gpsValidation.getMaxAccuracyMeters()) {
            log.debug("GPS data rejected - poor accuracy: {} meters for vehicle {}",
                    gpsData.accuracy(), gpsData.vehicleIdentifier());
            return false;
        }

        double speedKmh = gpsData.speed() * 3.6;
        if (speedKmh > gpsValidation.getMaxSpeedKmh()) {
            log.debug("GPS data rejected - unreasonable speed: {} km/h for vehicle {}",
                    speedKmh, gpsData.vehicleIdentifier());
            return false;
        }

        if (gpsValidation.isRejectNullIslandCoordinates()) {
            if (Math.abs(gpsData.latitude()) < 0.1 && Math.abs(gpsData.longitude()) < 0.1) {
                log.debug("GPS data rejected - null island coordinates for vehicle {}",
                        gpsData.vehicleIdentifier());
                return false;
            }
        }

        Timestamp dataTimestamp = Timestamp.of(gpsData.timestamp());
        Duration dataAge = Duration.between(dataTimestamp.toInstant(), Timestamp.now().toInstant());
        if (dataAge.compareTo(gpsValidation.getMaxGpsAge()) > 0) {
            log.debug("GPS data rejected - too old: {} for vehicle {}",
                    dataAge, gpsData.vehicleIdentifier());
            return false;
        }

        return true;
    }

    private boolean isSignificantUpdate(GpsDataProvider.GpsLocationData gpsData) {
        String vehicleId = gpsData.vehicleIdentifier();
        Timestamp currentTimestamp = Timestamp.of(gpsData.timestamp());

        Timestamp lastProcessed = lastProcessedTimestamps.get(vehicleId);
        if (lastProcessed != null) {
            Duration timeSinceLastUpdate = Duration.between(lastProcessed.toInstant(), currentTimestamp.toInstant());
            // ✅ ИСПОЛЬЗУЕМ РЕАЛЬНОЕ свойство
            Duration cooldown = config.getBusinessRules().getLocation().getLocationUpdateCooldown();

            if (timeSinceLastUpdate.compareTo(cooldown) < 0) {
                return false;
            }
        }

        lastProcessedTimestamps.put(vehicleId, currentTimestamp);
        return true;
    }

    public record ProviderProcessingResult(
            String providerName,
            int successfulUpdates,
            int failedUpdates,
            List<String> errors
    ) {
        public static ProviderProcessingResult withError(String providerName, Throwable error) {
            return new ProviderProcessingResult(providerName, 0, 1, List.of(error.getMessage()));
        }

        public static ProviderProcessingResult fromUpdates(List<LocationUpdateResult> results, String providerName) {
            int successful = (int) results.stream().filter(LocationUpdateResult::isSuccessful).count();
            int failed = results.size() - successful;
            List<String> errors = results.stream()
                    .filter(r -> !r.isSuccessful())
                    .map(r -> r.error() != null ? r.error().getMessage() : "Unknown error")
                    .toList();

            return new ProviderProcessingResult(providerName, successful, failed, errors);
        }
    }

    public record LocationUpdateResult(
            String vehicleIdentifier,
            java.time.Instant timestamp,
            boolean successful,
            Throwable error
    ) {
        public static LocationUpdateResult successful(String vehicleIdentifier, java.time.Instant timestamp) {
            return new LocationUpdateResult(vehicleIdentifier, timestamp, true, null);
        }

        public static LocationUpdateResult failed(String vehicleIdentifier, Throwable error) {
            return new LocationUpdateResult(vehicleIdentifier, java.time.Instant.now(), false, error);
        }

        public boolean isSuccessful() {
            return successful;
        }
    }
}