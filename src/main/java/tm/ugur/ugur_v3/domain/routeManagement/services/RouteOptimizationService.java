package tm.ugur.ugur_v3.domain.routeManagement.services;

import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.domain.geospatial.services.GeoSpatialService;
import tm.ugur.ugur_v3.domain.geospatial.valueobjects.Distance;
import tm.ugur.ugur_v3.domain.routeManagement.aggregate.Route;
import tm.ugur.ugur_v3.domain.routeManagement.aggregate.RouteSegment;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.EstimatedDuration;
import tm.ugur.ugur_v3.domain.shared.services.DomainService;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;

import java.util.List;
import java.util.Map;

public interface RouteOptimizationService extends DomainService {


    Mono<RouteOptimizationResult> optimizeStopSequence(Route route,
                                                       Map<StopId, GeoCoordinate> stopLocations,
                                                       OptimizationCriteria criteria);

    Mono<List<RouteSegment>> findOptimalPath(List<StopId> stopIds,
                                             Map<StopId, GeoCoordinate> stopLocations,
                                             GeoSpatialService geoSpatialService);

    Mono<FrequencyOptimizationResult> optimizeFrequency(Route route,
                                                        PassengerDemandDatd demandData,
                                                        VehicleCapacityConstraints constraints);

    Mono<List<RouteImprovement>> suggestImprovements(Route route,
                                                     RoutePerformanceMetrics performance,
                                                     List<PassengerFeedback> feedback);

    Mono<RouteEfficiencyScore> calculateEfficiency(Route route,
                                                   RouteOperationalData operationalData);


    record RouteOptimizationResult(
            List<StopId> optimizedSequence,
            Distance totalDistance,
            EstimatedDuration totalTime,
            double improvementPercentage,
            List<String> changesApplied,
            OptimizationMetrics metrics
    ) {}

    record FrequencyOptimizationResult(
            int recommendedHeadwayMinutes,
            int peakHeadwayMinutes,
            int offPeakHeadwayMinutes,
            double expectedLoadFactor,
            int estimatedDailyRidership
    ) {}

    record RouteImprovement(
            String improvementType,
            String description,
            double expectedBenefit,
            double implementationCost,
            int priorityScore
    ) {}

    record RouteEfficiencyScore(
            double overallScore,
            double timeEfficiency,
            double costEfficiency,
            double passengerSatisfaction,
            String performanceGrade
    ) {}

    record OptimizationMetrics(
            double fuelSavingsPercentage,
            int timeSavingsMinutes,
            double costReductionPercentage,
            int carbonEmissionReduction
    ) {}

    enum OptimizationCriteria {
        MINIMIZE_DISTANCE,
        MINIMIZE_TIME,
        MAXIMIZE_PASSENGER_CONVENIENCE,
        MINIMIZE_COST,
        BALANCED_APPROACH
    }
}