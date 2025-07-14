/**
 * COMPONENT: ETACalculationService Domain Service
 * LAYER: Domain/VehicleManagement/Services
 * PURPOSE: Complex ETA calculations with GPS integration - uses shared kernel DomainService
 * PERFORMANCE TARGET: < 50ms ETA calculation, supports 100K+ concurrent requests
 * SCALABILITY: Reactive implementation with WebFlux, cacheable results
 */
package tm.ugur.ugur_v3.domain.vehicleManagement.services;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.services.DomainService;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;
import tm.ugur.ugur_v3.domain.vehicleManagement.aggregate.Vehicle;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.Speed;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleType;

import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.List;
import java.util.Map;


public interface ETACalculationService extends DomainService {

    Mono<ETAResult> calculateETAToNextStop(Vehicle vehicle,
                                           GeoCoordinate nextStopLocation,
                                           String routeId);

    Mono<ETAResult> calculateETAToFinalStop(Vehicle vehicle,
                                            GeoCoordinate finalStopLocation,
                                            String routeId,
                                            List<GeoCoordinate> remainingStops);

    Mono<Map<String, ETAResult>> calculateETAForAllStops(Vehicle vehicle,
                                                         String routeId,
                                                         Map<String, GeoCoordinate> remainingStops);

    Mono<ETAResult> recalculateWithTrafficUpdate(Vehicle vehicle,
                                                 ETAResult previousETA,
                                                 double trafficDelayMinutes);

    Mono<Double> calculateETAAccuracy(VehicleType vehicleType,
                                      String routeId,
                                      Timestamp timeOfDay,
                                      String weatherConditions);

    Mono<ETATimeRange> predictArrivalTimeRange(Vehicle vehicle,
                                               GeoCoordinate targetLocation,
                                               String routeId);

    Mono<Map<VehicleId, List<ETAResult>>> optimizeRouteTimings(List<Vehicle> vehicles,
                                                               String routeId,
                                                               List<GeoCoordinate> targetStops);

    record ETAResult(
            Duration estimatedTime,
            Timestamp arrivalTime,
            double confidenceLevel,
            String calculationMethod,
            Map<String, Object> factors,
            ETAQuality quality
    ) {

        public boolean isReliable() {
            return confidenceLevel >= 0.75 && quality != ETAQuality.POOR;
        }

        public boolean indicatesDelay() {
            return factors.containsKey("trafficDelay") &&
                    (Double) factors.get("trafficDelay") > 5.0;
        }

        public String getDisplayTime() {
            if (estimatedTime.toMinutes() < 1) {
                return "Arriving now";
            } else if (estimatedTime.toMinutes() < 60) {
                return estimatedTime.toMinutes() + " min";
            } else {
                return String.format("%dh %dm",
                        estimatedTime.toHours(),
                        estimatedTime.toMinutes() % 60);
            }
        }

        public String getColorCode() {
            return quality.getColorCode();
        }
    }

    record ETATimeRange(
            Duration minimumTime,
            Duration maximumTime,
            Duration mostLikelyTime,
            double variance
    ) {

        public boolean isNarrowRange() {
            return maximumTime.minus(minimumTime).toMinutes() <= 5;
        }

        public String getRangeDescription() {
            if (isNarrowRange()) {
                return mostLikelyTime.toMinutes() + " min";
            } else {
                return minimumTime.toMinutes() + "-" + maximumTime.toMinutes() + " min";
            }
        }
    }

    @Getter
    enum ETAQuality {
        EXCELLENT("Excellent", "green", 95),
        GOOD("Good", "lightgreen", 85),
        FAIR("Fair", "yellow", 70),
        POOR("Poor", "red", 50);

        private final String description;
        private final String colorCode;
        private final int accuracyThreshold;

        ETAQuality(String description, String colorCode, int accuracyThreshold) {
            this.description = description;
            this.colorCode = colorCode;
            this.accuracyThreshold = accuracyThreshold;
        }

        public static ETAQuality fromConfidenceLevel(double confidenceLevel) {
            double percentage = confidenceLevel * 100;

            if (percentage >= EXCELLENT.accuracyThreshold) {
                return EXCELLENT;
            } else if (percentage >= GOOD.accuracyThreshold) {
                return GOOD;
            } else if (percentage >= FAIR.accuracyThreshold) {
                return FAIR;
            } else {
                return POOR;
            }
        }
    }

    @Getter
    enum CalculationMethod {
        BASIC_DISTANCE("Basic Distance Calculation"),
        HISTORICAL_AVERAGE("Historical Average Analysis"),
        REAL_TIME_TRAFFIC("Real-time Traffic Integration"),
        MACHINE_LEARNING("Machine Learning Prediction"),
        HYBRID("Hybrid Multi-factor Analysis");

        private final String description;

        CalculationMethod(String description) {
            this.description = description;
        }

    }
}