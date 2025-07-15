package tm.ugur.ugur_v3.application.vehicleManagement.commands.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.application.shared.executor.UseCaseExecutor;
import tm.ugur.ugur_v3.application.shared.monitoring.PerformanceMonitor;
import tm.ugur.ugur_v3.application.shared.validation.ValidatorService;
import tm.ugur.ugur_v3.application.vehicleManagement.commands.ChangeVehicleStatusCommand;
import tm.ugur.ugur_v3.application.vehicleManagement.commands.results.ChangeVehicleStatusResult;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;
import tm.ugur.ugur_v3.domain.vehicleManagement.exceptions.VehicleNotFoundException;
import tm.ugur.ugur_v3.domain.vehicleManagement.repository.VehicleRepository;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChangeVehicleStatusHandler implements UseCaseExecutor.CommandHandler<ChangeVehicleStatusCommand, Mono<ChangeVehicleStatusResult>> {

    private final VehicleRepository vehicleRepository;
    private final PerformanceMonitor performanceMonitor;
    private final ValidatorService validatorService;

    @Override
    public Mono<ChangeVehicleStatusResult> handle(ChangeVehicleStatusCommand command) {
        return performanceMonitor.timeReactive("vehicle.status.change", () ->
                processStatusChange(command)
        );
    }

    private Mono<ChangeVehicleStatusResult> processStatusChange(ChangeVehicleStatusCommand command) {
        Instant startTime = Instant.now();

        return validateCommand(command)
                .then(findVehicle(command.vehicleId()))
                .flatMap(vehicle -> validateAndChangeStatus(vehicle, command))
                .flatMap(this::saveVehicle)
                .map(result -> createSuccessResult(result.vehicle, result.previousStatus, command, startTime))
                .onErrorResume(throwable -> {
                    log.error("Failed to change vehicle status for {}: {}",
                            command.vehicleId(), throwable.getMessage(), throwable);
                    return Mono.just(handleError(command, throwable, startTime));
                });
    }

    private Mono<Void> validateCommand(ChangeVehicleStatusCommand command) {
        return Mono.fromRunnable(() -> {
            try {
                validatorService.validate(command);
                validateBusinessRules(command);
                log.debug("Command validation passed for vehicle: {}", command.vehicleId());
            } catch (Exception e) {
                log.warn("Command validation failed for vehicle {}: {}", command.vehicleId(), e.getMessage());
                throw new IllegalArgumentException("Command validation failed: " + e.getMessage(), e);
            }
        });
    }

    private void validateBusinessRules(ChangeVehicleStatusCommand command) {

        if (command.reason() == null || command.reason().trim().isEmpty()) {
            throw new IllegalArgumentException("Status change reason cannot be empty");
        }

        String reason = command.reason().trim();
        if (reason.length() < 5) {
            throw new IllegalArgumentException("Status change reason must be descriptive (at least 5 characters)");
        }

        String lowerReason = reason.toLowerCase();
        if (lowerReason.equals("test") || lowerReason.equals("update") || lowerReason.equals("change")) {
            throw new IllegalArgumentException("Status change reason must be specific, not generic");
        }


        if (command.changedBy() == null || command.changedBy().trim().isEmpty()) {
            throw new IllegalArgumentException("Status change must specify who made the change");
        }


        if (isCriticalStatusChange(command.newStatus()) && !command.changedBy().startsWith("ADMIN_")) {
            throw new IllegalArgumentException("Critical status changes require admin authorization");
        }
    }

    private boolean isCriticalStatusChange(VehicleStatus newStatus) {
        return newStatus == VehicleStatus.BREAKDOWN ||
               newStatus == VehicleStatus.RETIRED;
    }

    private Mono<Vehicle> findVehicle(VehicleId vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .switchIfEmpty(Mono.error(VehicleNotFoundException.byId(vehicleId)))
                .doOnNext(vehicle -> log.debug("Found vehicle: {} with current status: {}",
                        vehicleId, vehicle.getStatus()));
    }

    private Mono<StatusChangeResult> validateAndChangeStatus(Vehicle vehicle, ChangeVehicleStatusCommand command) {
        return Mono.fromCallable(() -> {
            VehicleStatus previousStatus = vehicle.getStatus();


            validateStatusTransition(previousStatus, command.newStatus(), vehicle, command.vehicleId());


            vehicle.changeStatus(command.newStatus(), command.reason(), command.changedBy());

            log.info("Vehicle {} status changed from {} to {} by {} (reason: {})",
                    command.vehicleId(),
                    previousStatus,
                    command.newStatus(),
                    command.changedBy(),
                    command.reason());

            return new StatusChangeResult(vehicle, previousStatus);
        });
    }

    private void validateStatusTransition(VehicleStatus currentStatus, VehicleStatus newStatus,
                                          Vehicle vehicle, VehicleId vehicleId) {

        if (currentStatus == newStatus) {
            throw new IllegalArgumentException(
                    String.format("Vehicle %s is already in %s status", vehicleId.getValue(), newStatus)
            );
        }


        switch (newStatus) {
            case IN_ROUTE -> {
                if (vehicle.getAssignedRouteId() == null) {
                    throw new IllegalArgumentException(
                            "Cannot set vehicle to IN_ROUTE status without assigned route"
                    );
                }
                if (currentStatus != VehicleStatus.ACTIVE) {
                    throw new IllegalArgumentException(
                            "Vehicle must be ACTIVE before transitioning to IN_ROUTE"
                    );
                }
            }
            case ACTIVE -> {
                if (vehicle.needsMaintenance()) {
                    throw new IllegalArgumentException(
                            "Cannot activate vehicle that requires maintenance"
                    );
                }
                if (currentStatus == VehicleStatus.RETIRED) {
                    throw new IllegalArgumentException(
                            "Cannot reactivate retired vehicle"
                    );
                }
            }
            case INACTIVE -> {
                if (currentStatus == VehicleStatus.IN_ROUTE) {
                    throw new IllegalArgumentException(
                            "Cannot transition directly from IN_ROUTE to INACTIVE. Must go through ACTIVE status first"
                    );
                }
            }
            case MAINTENANCE -> {
                if (currentStatus == VehicleStatus.IN_ROUTE) {
                    throw new IllegalArgumentException(
                            "Cannot send vehicle to maintenance while in route. Complete route first"
                    );
                }
            }
            case RETIRED -> {

                if (currentStatus == VehicleStatus.IN_ROUTE) {
                    throw new IllegalArgumentException(
                            "Cannot retire vehicle while in route"
                    );
                }
            }
        }
    }

    private Mono<StatusChangeResult> saveVehicle(StatusChangeResult result) {
        return vehicleRepository.save(result.vehicle)
                .map(savedVehicle -> new StatusChangeResult(savedVehicle, result.previousStatus))
                .doOnSuccess(saved -> {
                    log.debug("Saved vehicle {} with new status: {}",
                            saved.vehicle.getId(), saved.vehicle.getStatus());
                    performanceMonitor.incrementCounter("vehicle.status.changed.success");
                })
                .doOnError(error -> {
                    log.error("Failed to save vehicle {}: {}", result.vehicle.getId(), error.getMessage());
                    performanceMonitor.incrementCounter("vehicle.status.changed.error");
                });
    }

    private ChangeVehicleStatusResult createSuccessResult(Vehicle vehicle,
                                                          VehicleStatus previousStatus,
                                                          ChangeVehicleStatusCommand command,
                                                          Instant startTime) {
        Duration processingTime = Duration.between(startTime, Instant.now());


        boolean isAutomatic = "SYSTEM".equals(command.changedBy());

        if (isAutomatic) {
            return ChangeVehicleStatusResult.automaticSuccess(
                    command.vehicleId(),
                    previousStatus,
                    command.newStatus(),
                    processingTime,
                    command.reason()
            );
        } else {
            return ChangeVehicleStatusResult.successWithReason(
                    command.vehicleId(),
                    previousStatus,
                    command.newStatus(),
                    processingTime,
                    command.changedBy(),
                    command.reason()
            );
        }
    }

    private ChangeVehicleStatusResult handleError(ChangeVehicleStatusCommand command,
                                                  Throwable throwable,
                                                  Instant startTime) {
        performanceMonitor.incrementCounter("vehicle.status.change.error");

        String errorMessage = throwable.getMessage();
        if (errorMessage == null) {
            errorMessage = "Unknown error occurred during status change";
        }


        if (throwable instanceof VehicleNotFoundException) {
            return ChangeVehicleStatusResult.vehicleNotFound(command.vehicleId());
        } else if (throwable instanceof IllegalArgumentException) {

            if (errorMessage.contains("transition")) {
                return ChangeVehicleStatusResult.invalidTransition(
                        command.vehicleId(),
                        null,
                        command.newStatus(),
                        errorMessage
                );
            } else {
                return ChangeVehicleStatusResult.failure(command.vehicleId(), errorMessage);
            }
        } else {
            return ChangeVehicleStatusResult.failure(command.vehicleId(), errorMessage);
        }
    }

    private record StatusChangeResult(Vehicle vehicle, VehicleStatus previousStatus) {}
}