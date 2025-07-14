package tm.ugur.ugur_v3.domain.vehicleManagement.repository;

import tm.ugur.ugur_v3.domain.shared.repositories.Repository;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;
import tm.ugur.ugur_v3.domain.vehicleManagement.specifications.VehicleSpecification;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
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


    Flux<Vehicle> findByStatus(VehicleStatus status, int page, int size);

    Flux<Vehicle> findByAssignedRoute(String routeId);

    Flux<Vehicle> findAvailableForAssignment(Integer minCapacity);

    Flux<Vehicle> findRequiringMaintenance();

    Flux<Vehicle> findWithLowFuel(Double fuelThreshold);


    Flux<Vehicle> findWithinRadius(GeoCoordinate center, double radiusMeters);

    Flux<Vehicle> findNearestTo(GeoCoordinate location, int maxResults);

    Flux<Vehicle> findInBoundingBox(GeoCoordinate southWest, GeoCoordinate northEast);


    Flux<Vehicle> findWithStaleGpsData(Duration maxAge);

    Mono<java.util.Map<VehicleStatus, Long>> countByStatus();

    Flux<Vehicle> findByLicensePlateContaining(String licensePlate);


    Mono<Integer> updateStatusBatch(List<VehicleId> vehicleIds, VehicleStatus newStatus,
                                    String reason, String changedBy);

    Mono<Integer> updateLocationsBatch(List<LocationUpdate> locationUpdates);

    Mono<Integer> unassignFromRoute(String routeId, String reason);


    Flux<Vehicle> findBySpecification(VehicleSpecification specification);

    Mono<Long> countBySpecification(VehicleSpecification specification);


    Mono<VehicleUsageStats> getUsageStatistics(java.time.LocalDate fromDate,
                                               java.time.LocalDate toDate);

    Flux<RouteVehicleCount> getTopRoutesByVehicleCount(int limit);


    Mono<Void> warmupCache();

    Mono<Void> evictFromCache(VehicleId vehicleId);


    record LocationUpdate(
            VehicleId vehicleId,
            GeoCoordinate location,
            Double speed,
            Double bearing,
            java.time.Instant timestamp
    ) {}

    record VehicleUsageStats(
            int totalVehicles,
            int activeVehicles,
            int inRouteVehicles,
            double averageUtilization,
            java.time.Duration totalOperatingTime
    ) {}

    record RouteVehicleCount(
            String routeId,
            String routeName,
            int vehicleCount,
            double averageSpeed
    ) {}
}