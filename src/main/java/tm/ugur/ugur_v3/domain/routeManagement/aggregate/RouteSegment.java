package tm.ugur.ugur_v3.domain.routeManagement.aggregate;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.entities.Entity;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.Distance;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.EstimatedDuration;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteSegmentId;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;

import java.util.Objects;

@Getter
public class RouteSegment extends Entity<RouteSegmentId> {

    private static final double MAX_SEGMENT_DISTANCE_KM = 50.0;
    private static final double MIN_SEGMENT_DISTANCE_M = 50.0;
    private static final int MAX_SEGMENT_TIME_HOURS = 3;
    private static final int MIN_SEGMENT_TIME_SECONDS = 30;

    private final StopId fromStopId;
    private final StopId toStopId;
    private final Distance distance;
    private final EstimatedDuration estimatedTime;

    private final double averageSpeed;
    private final int trafficComplexity;
    private final boolean hasTrafficLights;
    private final boolean isUrbanArea;

    private final EstimatedDuration rushHourTime;
    private final EstimatedDuration offPeakTime;
    private final EstimatedDuration nightTime;

    public static RouteSegment create(StopId fromStopId, StopId toStopId,
                                      Distance distance, EstimatedDuration estimatedTime) {
        return new RouteSegment(
                RouteSegmentId.generate(),
                fromStopId,
                toStopId,
                distance,
                estimatedTime,
                calculateAverageSpeed(distance, estimatedTime),
                1,
                false,
                true,
                estimatedTime.multiply(1.3),
                estimatedTime,
                estimatedTime.multiply(0.8)
        );
    }

    public static RouteSegment createWithTrafficAnalysis(
            StopId fromStopId,
            StopId toStopId,
            Distance distance,
            EstimatedDuration baseTime,
            int trafficComplexity,
            boolean hasTrafficLights,
            boolean isUrbanArea) {
        validateTrafficParameters(trafficComplexity);

        EstimatedDuration adjustedBase = adjustForTrafficConditions(baseTime, trafficComplexity, hasTrafficLights, isUrbanArea);
        EstimatedDuration rushHour = calculateRushHourTime(adjustedBase, trafficComplexity, isUrbanArea);
        EstimatedDuration offPeak = adjustedBase;
        EstimatedDuration night = calculateNightTime(adjustedBase, isUrbanArea);

        return new RouteSegment(
                RouteSegmentId.generate(),
                fromStopId,
                toStopId,
                distance,
                adjustedBase,
                calculateAverageSpeed(distance, adjustedBase),
                trafficComplexity,
                hasTrafficLights,
                isUrbanArea,
                rushHour,
                offPeak,
                night
        );
    }

    private RouteSegment(RouteSegmentId segmentId, StopId fromStopId, StopId toStopId,
                         Distance distance, EstimatedDuration estimatedTime, double averageSpeed,
                         int trafficComplexity, boolean hasTrafficLights, boolean isUrbanArea,
                         EstimatedDuration rushHourTime, EstimatedDuration offPeakTime,
                         EstimatedDuration nightTime) {
        super(segmentId);

        this.fromStopId = validateStopId(fromStopId, "fromStopId");
        this.toStopId = validateStopId(toStopId, "toStopId");
        this.distance = validateDistance(distance);
        this.estimatedTime = validateEstimatedTime(estimatedTime);
        this.averageSpeed = validateAverageSpeed(averageSpeed);
        this.trafficComplexity = validateTrafficComplexity(trafficComplexity);
        this.hasTrafficLights = hasTrafficLights;
        this.isUrbanArea = isUrbanArea;
        this.rushHourTime = validateEstimatedTime(rushHourTime);
        this.offPeakTime = validateEstimatedTime(offPeakTime);
        this.nightTime = validateEstimatedTime(nightTime);

        validateStopsAreDifferent();
        validateSpeedConsistency();
    }


    public EstimatedDuration getEstimatedTimeForPeriod(TimePeriod period) {
        return switch (period) {
            case RUSH_HOUR -> rushHourTime;
            case OFF_PEAK -> offPeakTime;
            case NIGHT -> nightTime;
            case WEEKEND -> offPeakTime.multiply(0.9);
        };
    }

    public EstimatedDuration getAdjustedTime(double trafficFactor) {
        if (trafficFactor < 0.5 || trafficFactor > 3.0) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SEGMENT_TRAFFIC_FACTOR",
                    "Traffic factor must be between 0.5 and 3.0");
        }
        return estimatedTime.multiply(trafficFactor);
    }

    public double getSpeedForPeriod(TimePeriod period) {
        EstimatedDuration timeForPeriod = getEstimatedTimeForPeriod(period);
        return distance.getKilometers() / timeForPeriod.getHours();
    }

    public boolean isSlowSegment() {
        return averageSpeed < (isUrbanArea ? 15.0 : 25.0);
    }

    public boolean isHighTrafficComplexity() {
        return trafficComplexity >= 7;
    }

    public int getDifficultyScore() {
        int score = trafficComplexity;
        if (hasTrafficLights) score += 2;
        if (isUrbanArea) score += 1;
        if (distance.getKilometers() > 10) score += 1;
        return Math.min(score, 10);
    }

    public double getEstimatedFuelConsumption() {
        double baseFuel = isUrbanArea ? 12.0 : 8.0;
        double trafficMultiplier = 1.0 + (trafficComplexity * 0.1);
        if (hasTrafficLights) trafficMultiplier += 0.2;
        return baseFuel * trafficMultiplier;
    }


    public RouteSegment createReverse() {
        return new RouteSegment(
                RouteSegmentId.generate(),
                this.toStopId,
                this.fromStopId,
                this.distance,
                this.estimatedTime,
                this.averageSpeed,
                this.trafficComplexity,
                this.hasTrafficLights,
                this.isUrbanArea,
                this.rushHourTime,
                this.offPeakTime,
                this.nightTime
        );
    }

    public boolean connectsTo(RouteSegment other) {
        return this.toStopId.equals(other.fromStopId);
    }

    public boolean isAdjacentTo(RouteSegment other) {
        return connectsTo(other) || other.connectsTo(this);
    }

    public Distance getCombinedDistance(RouteSegment nextSegment) {
        if (!connectsTo(nextSegment)) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SEGMENT_COMBINED_DISTANCE",
                    "Segments are not connected");
        }
        return this.distance.add(nextSegment.distance);
    }

    public EstimatedDuration getCombinedTime(RouteSegment nextSegment) {
        if (!connectsTo(nextSegment)) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SEGMENT_COMBINED_TIME",
                    "Segments are not connected");
        }
        return this.estimatedTime.add(nextSegment.estimatedTime);
    }


    private static StopId validateStopId(StopId stopId, String fieldName) {
        if (stopId == null) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SEGMENT_STOP_ID",
                    fieldName + " cannot be null");
        }
        return stopId;
    }

    private static Distance validateDistance(Distance distance) {
        if (distance == null) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SEGMENT_DISTANCE",
                    "Distance cannot be null");
        }
        if (distance.getMeters() < MIN_SEGMENT_DISTANCE_M) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SEGMENT_DISTANCE_METERS",
                    "Segment distance too short: " + distance.getMeters() + "m, minimum: " + MIN_SEGMENT_DISTANCE_M + "m"
            );
        }
        if (distance.getKilometers() > MAX_SEGMENT_DISTANCE_KM) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SEGMENT_DISTANCE_KM",
                    "Segment distance too long: " + distance.getKilometers() + "km, maximum: " + MAX_SEGMENT_DISTANCE_KM + "km"
            );
        }
        return distance;
    }

    private static EstimatedDuration validateEstimatedTime(EstimatedDuration time) {
        if (time == null) {
            throw new BusinessRuleViolationException("ROUTE_SEGMENT_ESTIMATE_DURATION", "Estimated time cannot be null");
        }
        if (time.getSeconds() < MIN_SEGMENT_TIME_SECONDS) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SEGMENT_TIME_SECONDS",
                    "Segment time too short: " + time.getSeconds() + "s, minimum: " + MIN_SEGMENT_TIME_SECONDS + "s"
            );
        }
        if (time.getHours() > MAX_SEGMENT_TIME_HOURS) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SEGMENT_TIME_HOURS",
                    "Segment time too long: " + time.getHours() + "h, maximum: " + MAX_SEGMENT_TIME_HOURS + "h"
            );
        }
        return time;
    }

    private static double validateAverageSpeed(double speed) {
        if (speed < 1.0 || speed > 120.0) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SEGMENT_AVERAGE_SPEED",
                    "Invalid average speed: " + speed + " km/h");
        }
        return speed;
    }

    private static int validateTrafficComplexity(int complexity) {
        if (complexity < 1 || complexity > 10) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SEGMENT_TRAFFIC_COMPLEXITY",
                    "Traffic complexity must be between 1 and 10");
        }
        return complexity;
    }

    private void validateStopsAreDifferent() {
        if (fromStopId.equals(toStopId)) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SEGMENT_STOP_DIFFERENT",
                    "From and to stops cannot be the same");
        }
    }

    private void validateSpeedConsistency() {
        double calculatedSpeed = distance.getKilometers() / estimatedTime.getHours();
        double speedDifference = Math.abs(calculatedSpeed - averageSpeed);
        if (speedDifference > 5.0) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SEGMENT_SPEED_CONSISTENCY",
                    "Speed inconsistency detected. Calculated: " + calculatedSpeed +
                            " km/h, provided: " + averageSpeed + " km/h"
            );
        }
    }

    private static void validateTrafficParameters(int trafficComplexity) {
        if (trafficComplexity < 1 || trafficComplexity > 10) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SEGMENT_TRAFFIC_PARAMETERS",
                    "Traffic complexity must be between 1 and 10");
        }
    }


    private static double calculateAverageSpeed(Distance distance, EstimatedDuration time) {
        if (time.getHours() <= 0) {
            throw new BusinessRuleViolationException(
                    "ROUTE_SEGMENT_AVERAGE_SPEED",
                    "Time must be positive for speed calculation");
        }
        return distance.getKilometers() / time.getHours();
    }

    private static EstimatedDuration adjustForTrafficConditions(EstimatedDuration baseTime,
                                                                int trafficComplexity,
                                                                boolean hasTrafficLights,
                                                                boolean isUrbanArea) {
        double multiplier = 1.0;

        multiplier += (trafficComplexity - 1) * 0.1;

        if (hasTrafficLights) {
            multiplier += 0.15;
        }

        if (isUrbanArea) {
            multiplier += 0.1;
        }

        return baseTime.multiply(multiplier);
    }

    private static EstimatedDuration calculateRushHourTime(EstimatedDuration baseTime,
                                                           int trafficComplexity,
                                                           boolean isUrbanArea) {
        double rushMultiplier = 1.2; // Base 20% increase

        if (isUrbanArea) {
            rushMultiplier += 0.3; // Additional 30% for urban
        }

        if (trafficComplexity >= 7) {
            rushMultiplier += 0.2; // Additional 20% for high complexity
        }

        return baseTime.multiply(rushMultiplier);
    }

    private static EstimatedDuration calculateNightTime(EstimatedDuration baseTime, boolean isUrbanArea) {
        double nightMultiplier = isUrbanArea ? 0.85 : 0.75; // Urban: -15%, Rural: -25%
        return baseTime.multiply(nightMultiplier);
    }


    public enum TimePeriod {
        RUSH_HOUR,
        OFF_PEAK,
        NIGHT,
        WEEKEND
    }


    @Override
    public String toString() {
        return String.format("RouteSegment{id=%s, from=%s, to=%s, distance=%s, time=%s, speed=%.1f km/h}",
                getId(), fromStopId, toStopId, distance, estimatedTime, averageSpeed);
    }
}