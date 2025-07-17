package tm.ugur.ugur_v3.domain.routeManagement.services;

import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.domain.routeManagement.aggregate.Route;
import tm.ugur.ugur_v3.domain.routeManagement.aggregate.RouteSchedule;
import tm.ugur.ugur_v3.domain.routeManagement.enums.ServiceFrequency;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.DailySchedule;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.SchedulePeriod;
import tm.ugur.ugur_v3.domain.shared.services.DomainService;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public interface SchedulePlanningService extends DomainService {

    Mono<SchedulePlanningResult> createOptimalSchedule(Route route,
                                                       PassengerDemandProfile demandProfile,
                                                       VehicleAvailability vehicleAvailability,
                                                       OperationalConstraints constraints);

    Mono<DynamicHeadwayPlan> calculateDynamicHeadways(Route route,
                                                      RealTimePassengerData passengerData,
                                                      VehicleCapacityData capacityData);

    Mono<ScheduleAdaptationResult> adaptScheduleToConditions(RouteSchedule currentSchedule,
                                                             WeatherConditions weather,
                                                             TrafficConditions traffic,
                                                             SpecialEvents events);

    Mono<ScheduleEfficiencyAnalysis> analyzeScheduleEfficiency(RouteSchedule schedule,
                                                               ActualPerformanceData performance);

    Mono<TransferOptimizationResult> optimizeTransfers(List<Route> connectingRoutes,
                                                       Map<RouteId, RouteSchedule> schedules,
                                                       PassengerTransferData transferData);


    record SchedulePlanningResult(
            RouteSchedule proposedSchedule,
            ServiceFrequency recommendedFrequency,
            Map<DayOfWeek, DailySchedule> weeklySchedule,
            List<SchedulePeriod> specialPeriods,
            ScheduleQualityMetrics qualityMetrics
    ) {}

    record DynamicHeadwayPlan(
            Map<LocalTime, Integer> hourlyHeadways,
            int baseHeadwayMinutes,
            int peakReductionPercentage,
            List<HeadwayAdjustment> adjustments
    ) {}

    record ScheduleAdaptationResult(
            RouteSchedule adaptedSchedule,
            List<ScheduleModification> modifications,
            double reliabilityImpact,
            String adaptationReason
    ) {}

    record ScheduleEfficiencyAnalysis(
            double adherenceScore,
            double passengerSatisfactionScore,
            double resourceUtilizationScore,
            List<EfficiencyRecommendation> recommendations
    ) {}

    record TransferOptimizationResult(
            Map<RouteId, List<LocalTime>> synchronizedDepartures,
            double averageTransferWaitTime,
            int improvedConnections,
            PassengerBenefitEstimate benefitEstimate
    ) {}

    record PassengerDemandProfile(
            Map<LocalTime, Integer> hourlyDemand,
            Map<DayOfWeek, Integer> weeklyPattern,
            SeasonalVariation seasonalPattern,
            List<DemandPeak> peakPeriods
    ) {}

    record VehicleAvailability(
            int totalVehicles,
            int peakHourVehicles,
            int maintenanceVehicles,
            Map<String, Integer> vehicleTypeAvailability
    ) {}

    record OperationalConstraints(
            LocalTime earliestStartTime,
            LocalTime latestEndTime,
            int minHeadwayMinutes,
            int maxHeadwayMinutes,
            int driverShiftDurationHours,
            double budgetConstraint
    ) {}
}