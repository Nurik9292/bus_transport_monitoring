package tm.ugur.ugur_v3.application.vehicleManagement.commands.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.application.shared.executor.UseCaseExecutor;
import tm.ugur.ugur_v3.application.shared.monitoring.PerformanceMonitor;
import tm.ugur.ugur_v3.application.shared.validation.ValidatorService;
import tm.ugur.ugur_v3.application.vehicleManagement.commands.UpdateVehicleLocationCommand;
import tm.ugur.ugur_v3.application.vehicleManagement.commands.results.UpdateVehicleLocationResult;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.exceptions.VehicleNotFoundException;
import tm.ugur.ugur_v3.domain.vehicleManagement.repository.VehicleRepository;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.Bearing;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.Speed;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateVehicleLocationHandler implements UseCaseExecutor.CommandHandler<UpdateVehicleLocationCommand, Mono<UpdateVehicleLocationResult>> {

    private final VehicleRepository vehicleRepository;
    private final PerformanceMonitor performanceMonitor;
    private final ValidatorService validatorService;

    @Override
    public Mono<UpdateVehicleLocationResult> handle(UpdateVehicleLocationCommand command) {
        Instant startTime = Instant.now();

        return performanceMonitor.timeReactive("vehicle.location.update", () ->
                processLocationUpdate(command, startTime)
        );
    }

    private Mono<UpdateVehicleLocationResult> processLocationUpdate(UpdateVehicleLocationCommand command,
                                                                    Instant startTime) {
        return validateCommand(command)
                .then(findVehicle(command))
                .flatMap(vehicle -> updateVehicleLocation(vehicle, command))
                .flatMap(vehicle -> saveVehicle(vehicle, command))
                .map(vehicle -> createSuccessResult(vehicle, command, startTime))
                .onErrorResume(throwable -> handleError(command, startTime, throwable));
    }

    private Mono<Void> validateCommand(UpdateVehicleLocationCommand command) {
        return Mono.fromRunnable(() -> {
            try {
                validatorService.validate(command);
                log.debug("Location update command validation passed for vehicle: {}", command.vehicleId());
            } catch (Exception e) {
                log.warn("Location update command validation failed: {}", e.getMessage());
                throw new IllegalArgumentException("Command validation failed: " + e.getMessage(), e);
            }
        });
    }

    private Mono<Vehicle> findVehicle(UpdateVehicleLocationCommand command) {
        return vehicleRepository.findById(command.vehicleId())
                .switchIfEmpty(Mono.error(VehicleNotFoundException.byId(command.vehicleId())))
                .doOnNext(vehicle -> log.debug("Found vehicle for location update: {}", command.vehicleId()));
    }

    private Mono<Vehicle> updateVehicleLocation(Vehicle vehicle, UpdateVehicleLocationCommand command) {
        return Mono.fromCallable(() -> {
            // Validate GPS coordinates first
            GeoCoordinate location = command.location();
            if (location == null) {
                throw new IllegalArgumentException("Location cannot be null");
            }

            // Create Speed and Bearing objects
            Speed speed = command.speedKmh() != null ?
                    Speed.ofKmh(command.speedKmh()) : Speed.zero();
            Bearing bearing = command.bearingDegrees() != null ?
                    Bearing.ofDegrees(command.bearingDegrees()) : Bearing.north();

            // Store previous location for result
            GeoCoordinate previousLocation = vehicle.getCurrentLocation();

            // Update vehicle location
            vehicle.updateLocation(location, speed, bearing);

            log.debug("Updated location for vehicle: {} from {} to {} (speed: {} km/h, bearing: {}°)",
                    vehicle.getId(),
                    previousLocation != null ? previousLocation.toString() : "null",
                    location.toString(),
                    speed.getKmh(),
                    bearing.getDegrees());

            return vehicle;
        });
    }

    private Mono<Vehicle> saveVehicle(Vehicle vehicle, UpdateVehicleLocationCommand command) {
        return vehicleRepository.save(vehicle)
                .doOnSuccess(savedVehicle -> {
                    performanceMonitor.incrementCounter("vehicle.location.updated.success");
                    log.info("Successfully updated location for vehicle {}", command.vehicleId());
                })
                .doOnError(error -> {
                    performanceMonitor.incrementCounter("vehicle.location.updated.error");
                    log.error("Failed to save vehicle {} after location update: {}",
                            command.vehicleId(), error.getMessage());
                });
    }

    private UpdateVehicleLocationResult createSuccessResult(Vehicle vehicle,
                                                            UpdateVehicleLocationCommand command,
                                                            Instant startTime) {
        Duration processingTime = Duration.between(startTime, Instant.now());

        GeoCoordinate newLocation = vehicle.getCurrentLocation();
        GeoCoordinate previousLocation = vehicle.getPreviousLocation();

        boolean isFirstUpdate = previousLocation == null;

        if (isFirstUpdate) {
            return UpdateVehicleLocationResult.firstLocationUpdate(
                    command.vehicleId(),
                    newLocation,
                    processingTime
            );
        } else {
            return UpdateVehicleLocationResult.successWithMovement(
                    command.vehicleId(),
                    previousLocation,
                    newLocation,
                    command.speedKmh(),
                    command.bearingDegrees(),
                    processingTime
            );
        }
    }

    private Mono<UpdateVehicleLocationResult> handleError(UpdateVehicleLocationCommand command,
                                                          Instant startTime,
                                                          Throwable throwable) {
        Duration processingTime = Duration.between(startTime, Instant.now());
        performanceMonitor.incrementCounter("vehicle.location.update.error");

        String errorMessage = throwable.getMessage();
        if (errorMessage == null) {
            errorMessage = "Unknown error occurred during location update";
        }

        log.error("Location update failed for vehicle {}: {}", command.vehicleId(), errorMessage, throwable);

        UpdateVehicleLocationResult errorResult;

        if (throwable instanceof VehicleNotFoundException) {
            errorResult = UpdateVehicleLocationResult.failure(
                    command.vehicleId(),
                    "Vehicle not found: " + command.vehicleId().getValue()
            );
        } else if (throwable instanceof IllegalArgumentException) {
            if (command.location() != null) {
                errorResult = UpdateVehicleLocationResult.invalidCoordinates(
                        command.vehicleId(),
                        command.location().getLatitude(),
                        command.location().getLongitude(),
                        errorMessage
                );
            } else {
                errorResult = UpdateVehicleLocationResult.failure(command.vehicleId(), errorMessage);
            }
        } else {
            errorResult = UpdateVehicleLocationResult.failure(command.vehicleId(), errorMessage);
        }

        return Mono.just(errorResult);
    }
}