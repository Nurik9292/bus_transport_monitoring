package tm.ugur.ugur_v3.domain.routeManagement.services;

import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.domain.routeManagement.aggregate.Route;
import tm.ugur.ugur_v3.domain.routeManagement.aggregate.RouteSchedule;
import tm.ugur.ugur_v3.domain.routeManagement.services.supporting.*;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
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

}