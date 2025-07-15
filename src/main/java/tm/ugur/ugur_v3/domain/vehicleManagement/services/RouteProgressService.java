package tm.ugur.ugur_v3.domain.vehicleManagement.services;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.services.DomainService;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.Speed;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public interface RouteProgressService extends DomainService {

    Mono<RouteProgress> calculateRouteProgress(Vehicle vehicle,
                                               String routeId,
                                               List<GeoCoordinate> routeGeometry);

    Mono<StopDetectionResult> detectStopEvents(Vehicle vehicle,
                                               List<RouteStop> routeStops,
                                               double proximityThresholdMeters);

    Mono<Double> calculateCompletionPercentage(Vehicle vehicle,
                                               String routeId,
                                               List<GeoCoordinate> routeGeometry);

    Mono<InterStopProgress> calculateInterStopProgress(Vehicle vehicle,
                                                       RouteStop currentStop,
                                                       RouteStop nextStop);

    Mono<RouteAdherence> analyzeRouteAdherence(Vehicle vehicle,
                                               String routeId,
                                               List<GeoCoordinate> plannedRoute,
                                               List<GeoCoordinate> actualPath);

    Mono<RemainingRouteInfo> calculateRemainingRoute(Vehicle vehicle,
                                                     String routeId,
                                                     List<RouteStop> remainingStops);

    Mono<RoutePerformanceMetrics> monitorRoutePerformance(Vehicle vehicle,
                                                          String routeId,
                                                          Map<String, Timestamp> scheduledTimes);

    Mono<Duration> predictRouteCompletionTime(Vehicle vehicle,
                                              String routeId,
                                              RouteProgress currentProgress);

    Mono<DwellTimeAnalysis> analyzeDwellTime(Vehicle vehicle,
                                             String stopId,
                                             Timestamp arrivalTime,
                                             Timestamp departureTime);

    Mono<RouteEfficiency> calculateRouteEfficiency(VehicleId vehicleId,
                                                   String routeId,
                                                   Timestamp startTime,
                                                   Timestamp endTime);


    record RouteProgress(
            String routeId,
            VehicleId vehicleId,
            double completionPercentage,
            double distanceTraveledKm,
            double remainingDistanceKm,
            int stopsCompleted,
            int totalStops,
            RouteStop currentNearestStop,
            RouteStop nextStop,
            boolean isOnRoute,
            double routeDeviationMeters,
            Timestamp calculatedAt
    ) {
        public RouteProgress {
            if (completionPercentage < 0 || completionPercentage > 100) {
                throw new IllegalArgumentException("Completion percentage must be between 0 and 100");
            }
            if (distanceTraveledKm < 0 || remainingDistanceKm < 0) {
                throw new IllegalArgumentException("Distances cannot be negative");
            }
        }

        public boolean isNearlyComplete() {
            return completionPercentage >= 90.0;
        }

        public boolean isOffRoute() {
            return !isOnRoute || routeDeviationMeters > 200.0;
        }
    }

    record RouteStop(
            String stopId,
            String stopName,
            GeoCoordinate location,
            int sequenceNumber,
            Timestamp scheduledArrival,
            Timestamp scheduledDeparture,
            boolean isMajorStop,
            double platformRadius
    ) {
        public boolean isWithinPlatform(GeoCoordinate vehicleLocation) {
            return location.distanceTo(vehicleLocation) <= platformRadius;
        }
    }

    @Getter
    enum StopEventType {
        APPROACHING("Approaching Stop"),
        ARRIVED("Arrived at Stop"),
        DEPARTED("Departed from Stop"),
        PASSED("Passed Stop Without Stopping");

        private final String description;

        StopEventType(String description) {
            this.description = description;
        }

    }

    record StopDetectionResult(
            RouteStop stop,
            StopEventType eventType,
            double distanceToStopMeters,
            Speed vehicleSpeed,
            Timestamp detectedAt,
            boolean isConfirmed
    ) {
        public boolean isArrival() {
            return eventType == StopEventType.ARRIVED;
        }

        public boolean isDeparture() {
            return eventType == StopEventType.DEPARTED;
        }
    }

    record InterStopProgress(
            RouteStop fromStop,
            RouteStop toStop,
            double progressPercentage,
            double distanceTraveledMeters,
            double remainingDistanceMeters,
            Duration estimatedTimeToNext,
            boolean isDelayed
    ) {
        public boolean isHalfway() {
            return progressPercentage >= 45.0 && progressPercentage <= 55.0;
        }
    }

    record RouteAdherence(
            String routeId,
            VehicleId vehicleId,
            double adherenceScore,
            double averageDeviationMeters,
            double maxDeviationMeters,
            List<GeoCoordinate> significantDeviations,
            boolean hasServiceIssues,
            String analysisNotes
    ) {
        public boolean isAcceptableAdherence() {
            return adherenceScore >= 80.0 && averageDeviationMeters <= 50.0;
        }
    }

    record RemainingRouteInfo(
            double remainingDistanceKm,
            Duration estimatedRemainingTime,
            int stopsRemaining,
            List<RouteStop> upcomingStops,
            RouteStop finalDestination,
            boolean isOnSchedule
    ) {
        public boolean isNearlyFinished() {
            return stopsRemaining <= 2 || remainingDistanceKm <= 5.0;
        }
    }

    record RoutePerformanceMetrics(
            String routeId,
            VehicleId vehicleId,
            double onTimePerformance,
            double averageDelayMinutes,
            double maxDelayMinutes,
            Speed averageSpeed,
            int stopsOnTime,
            int stopsDelayed,
            List<String> delayReasons
    ) {
        public boolean meetsServiceStandards() {
            return onTimePerformance >= 85.0 && averageDelayMinutes <= 3.0;
        }
    }

    record DwellTimeAnalysis(
            String stopId,
            VehicleId vehicleId,
            Duration actualDwellTime,
            Duration scheduledDwellTime,
            Duration varianceFromSchedule,
            boolean isExcessive,
            DwellTimeCategory category,
            String reasonCode
    ) {
        public boolean isWithinAcceptableRange() {
            return Math.abs(varianceFromSchedule.toSeconds()) <= 60; // ±1 minute
        }
    }

    @Getter
    enum DwellTimeCategory {
        QUICK("Quick", 30),
        NORMAL("Normal", 120),
        EXTENDED("Extended", 300),
        EXCESSIVE("Excessive", -1);

        private final String description;
        private final int maxSeconds;

        DwellTimeCategory(String description, int maxSeconds) {
            this.description = description;
            this.maxSeconds = maxSeconds;
        }


        public static DwellTimeCategory fromDuration(Duration duration) {
            long seconds = duration.getSeconds();
            if (seconds <= 30) return QUICK;
            if (seconds <= 120) return NORMAL;
            if (seconds <= 300) return EXTENDED;
            return EXCESSIVE;
        }
    }

    record RouteEfficiency(
            String routeId,
            VehicleId vehicleId,
            double efficiencyScore,
            Duration actualTime,
            Duration optimalTime,
            double actualDistanceKm,
            double optimalDistanceKm,
            Speed averageSpeed,
            Speed optimalSpeed,
            List<String> inefficiencyFactors
    ) {
        public double getTimeEfficiency() {
            return optimalTime.toSeconds() / (double) actualTime.toSeconds() * 100.0;
        }

        public double getDistanceEfficiency() {
            return optimalDistanceKm / actualDistanceKm * 100.0;
        }

        public boolean isEfficient() {
            return efficiencyScore >= 85.0;
        }
    }
}