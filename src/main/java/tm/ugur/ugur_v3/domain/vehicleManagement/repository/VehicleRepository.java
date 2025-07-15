package tm.ugur.ugur_v3.domain.vehicleManagement.repository;

import tm.ugur.ugur_v3.domain.shared.repositories.Repository;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.LicensePlate;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleType;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;

public interface VehicleRepository extends Repository<Vehicle, VehicleId> {


    @Override
    Mono<Vehicle> save(Vehicle vehicle);

    @Override
    Mono<Vehicle> findById(VehicleId vehicleId);

    @Override
    Mono<Boolean> existsById(VehicleId vehicleId);

    @Override
    Mono<Void> deleteById(VehicleId vehicleId);

    @Override
    Flux<Vehicle> findAll();


    Mono<Vehicle> findByLicensePlate(LicensePlate licensePlate);

    Flux<Vehicle> findByStatus(VehicleStatus status);

    Flux<Vehicle> findByVehicleType(VehicleType vehicleType);

    Flux<Vehicle> findByAssignedRouteId(String routeId);

    Flux<Vehicle> findAvailableForAssignment();

    Flux<Vehicle> findRequiringAttention();

    Flux<Vehicle> findMaintenanceDue();

    Flux<Vehicle> findWithStaleGpsData(Timestamp cutoffTime);


    Flux<Vehicle> findWithinRadius(GeoCoordinate center, double radiusMeters);

    Flux<Vehicle> findWithinBoundingBox(GeoCoordinate southwest, GeoCoordinate northeast);

    Flux<Vehicle> findNearestVehicles(GeoCoordinate location, int maxCount, double maxDistanceMeters);

    Flux<Vehicle> findCurrentlyMoving();

    Flux<Vehicle> findAtDepot();


    Mono<Long> countByStatus(VehicleStatus status);

    Mono<Long> countByVehicleType(VehicleType vehicleType);

    Flux<Vehicle> findWithGpsUpdatedSince(Timestamp since);

    Flux<Vehicle> findByStatusIn(List<VehicleStatus> statuses);

    Flux<Vehicle> findByVehicleTypeIn(List<VehicleType> vehicleTypes);


    Flux<Vehicle> saveAll(Flux<Vehicle> vehicles);

    Mono<Long> updateLocations(Flux<VehicleLocationUpdate> locationUpdates);

    Mono<Long> updateStatuses(Flux<VehicleStatusUpdate> statusUpdates);


    Mono<VehicleUtilizationStats> getUtilizationStats(Timestamp startTime, Timestamp endTime);

    Flux<RouteVehicleMapping> getVehiclesByRoute();

    Mono<MaintenanceStats> getMaintenanceStats(Timestamp startTime, Timestamp endTime);


    record VehicleLocationUpdate(
            VehicleId vehicleId,
            GeoCoordinate location,
            Double speedKmh,
            Double bearingDegrees,
            Timestamp timestamp
    ) {}

    record VehicleStatusUpdate(
            VehicleId vehicleId,
            VehicleStatus newStatus,
            String reason,
            String changedBy,
            Timestamp timestamp
    ) {}

    record VehicleUtilizationStats(
            int totalVehicles,
            int activeVehicles,
            int inRouteVehicles,
            double averageUtilization,
            double totalDistanceKm,
            double averageSpeedKmh,
            long totalOperatingHours
    ) {}

    record RouteVehicleMapping(
            String routeId,
            String routeName,
            List<VehicleId> assignedVehicles,
            int vehicleCount,
            double averageCapacityUtilization
    ) {}

    record MaintenanceStats(
            int totalMaintenanceEvents,
            int scheduledMaintenance,
            int emergencyMaintenance,
            double averageMaintenanceDurationHours,
            double maintenanceCostEstimate,
            List<VehicleId> overdueMaintenance
    ) {}
}