package tm.ugur.ugur_v3.domain.routeManagement.services;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.domain.geospatial.valueobjects.Distance;
import tm.ugur.ugur_v3.domain.routeManagement.aggregate.Route;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.EstimatedDuration;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.services.DomainService;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

import java.util.List;

public interface RouteMatchingService extends DomainService {

    Mono<RouteMatchResult> matchVehicleToRoute(VehicleId vehicleId,
                                               GeoCoordinate currentLocation,
                                               List<GeoCoordinate> recentPath,
                                               List<Route> candidateRoutes);

    Mono<StopMatchResult> findNearestStopOnRoute(Route route,
                                                 GeoCoordinate location,
                                                 Distance maxSearchRadius);

    Mono<RouteProgressResult> calculateRouteProgress(Route route,
                                                     GeoCoordinate currentLocation,
                                                     StopId lastKnownStop);

    Flux<AlternativeRouteOption> findAlternativeRoutes(GeoCoordinate origin,
                                                       GeoCoordinate destination,
                                                       RouteSelectionCriteria criteria,
                                                       List<Route> availableRoutes);

    Mono<VehicleRouteCompatibility> checkVehicleRouteCompatibility(VehicleId vehicleId,
                                                                   Route route,
                                                                   VehicleCapabilities vehicleCapabilities);


    record RouteMatchResult(
            RouteId matchedRouteId,
            double confidenceScore,
            Distance distanceToRoute,
            RouteDirection estimatedDirection,
            List<MatchingFactor> matchingFactors
    ) {}

    record StopMatchResult(
            StopId stopId,
            Distance distanceToStop,
            boolean isAtStop,
            StopApproachDirection approachDirection,
            EstimatedArrivalTime estimatedArrival
    ) {}

    record RouteProgressResult(
            double completionPercentage,
            StopId nextStop,
            Distance distanceToNextStop,
            EstimatedDuration timeToNextStop,
            int stopsRemaining
    ) {}

    record AlternativeRouteOption(
            RouteId routeId,
            Distance totalDistance,
            EstimatedDuration totalTime,
            int transfersRequired,
            double costEstimate,
            RouteQualityScore qualityScore
    ) {}

    record VehicleRouteCompatibility(
            boolean isCompatible,
            List<String> compatibilityIssues,
            CompatibilityScore compatibilityScore,
            List<String> recommendations
    ) {}

    enum RouteDirection {
        FORWARD, BACKWARD, UNKNOWN
    }

    enum StopApproachDirection {
        APPROACHING, AT_STOP, DEPARTING, PASSED
    }

    record MatchingFactor(
            String factorName,
            double weight,
            double score,
            String description
    ) {}
}