package tm.ugur.ugur_v3.domain.routeManagement.aggregate;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.geospatial.valueobjects.Distance;
import tm.ugur.ugur_v3.domain.shared.entities.Entity;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.EstimatedDuration;
import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteSegmentId;
import tm.ugur.ugur_v3.domain.stopManagement.valueobjects.StopId;


@Getter
public class RouteSegment extends Entity<RouteSegmentId> {


    private static final double MAX_SEGMENT_DISTANCE_KM = 50.0;
    private static final double MIN_SEGMENT_DISTANCE_M = 50.0;
    private static final int MAX_SEGMENT_TIME_HOURS = 3;
    private static final int MIN_SEGMENT_TIME_SECONDS = 30;
    private static final double MIN_SPEED_KMH = 5.0;
    private static final double MAX_SPEED_KMH = 80.0;

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


    private RouteSegment(
            RouteSegmentId segmentId,
            StopId fromStopId,
            StopId toStopId,
            Distance distance,
            EstimatedDuration estimatedTime,
            double averageSpeed,
            int trafficComplexity,
            boolean hasTrafficLights,
            boolean isUrbanArea,
            EstimatedDuration rushHourTime,
            EstimatedDuration offPeakTime,
            EstimatedDuration nightTime) {
        super(segmentId);

        this.fromStopId = validateStopId(fromStopId, "fromStopId");
        this.toStopId = validateStopId(toStopId, "toStopId");
        this.distance = validateDistance(distance);
        this.estimatedTime = validateEstimatedTime(estimatedTime);
        this.averageSpeed = validateAverageSpeed(averageSpeed, distance, estimatedTime);
        this.trafficComplexity = trafficComplexity;
        this.hasTrafficLights = hasTrafficLights;
        this.isUrbanArea = isUrbanArea;
        this.rushHourTime = rushHourTime;
        this.offPeakTime = offPeakTime;
        this.nightTime = nightTime;

        validateSegmentConstraints();
    }


    public static RouteSegment create(StopId fromStopId,
                                      StopId toStopId,
                                      Distance distance,
                                      EstimatedDuration estimatedTime) {
        return new RouteSegment(
                RouteSegmentId.generate(),
                fromStopId,
                toStopId,
                distance,
                estimatedTime,
                calculateAverageSpeed(distance, estimatedTime),
                5,
                true,
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

        EstimatedDuration adjustedBase = adjustForTrafficConditions(
                baseTime, trafficComplexity, hasTrafficLights, isUrbanArea);
        EstimatedDuration rushHour = calculateRushHourTime(
                adjustedBase, trafficComplexity, isUrbanArea);
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


    public EstimatedDuration getTimeForPeriod(TimePeriod period) {
        return switch (period) {
            case RUSH_HOUR -> rushHourTime;
            case OFF_PEAK -> offPeakTime;
            case NIGHT -> nightTime;
            case WEEKEND -> offPeakTime.multiply(0.9);
        };
    }

    public int getDifficultyScore() {
        int score = trafficComplexity;
        if (hasTrafficLights) score += 2;
        if (isUrbanArea) score += 1;
        if (distance.toKilometers() > 10) score += 1;
        return Math.min(score, 10);
    }

    public double getEstimatedFuelConsumption() {
        double baseFuel = isUrbanArea ? 12.0 : 8.0; // urban vs highway
        double trafficMultiplier = 1.0 + (trafficComplexity * 0.1);
        if (hasTrafficLights) trafficMultiplier += 0.2;
        return baseFuel * trafficMultiplier;
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
                    "SEGMENTS_NOT_CONNECTED",
                    "Segments are not connected: " + this.toStopId + " -> " + nextSegment.fromStopId);
        }
        return this.distance.add(nextSegment.distance);
    }

    public EstimatedDuration getCombinedTime(RouteSegment nextSegment) {
        return getCombinedTime(nextSegment, TimePeriod.OFF_PEAK);
    }

    public EstimatedDuration getCombinedTime(RouteSegment nextSegment, TimePeriod period) {
        if (!connectsTo(nextSegment)) {
            throw new BusinessRuleViolationException(
                    "SEGMENTS_NOT_CONNECTED",
                    "Segments are not connected for time calculation");
        }
        return this.getTimeForPeriod(period).add(nextSegment.getTimeForPeriod(period));
    }

    public boolean isHighTrafficComplexity() {
        return trafficComplexity >= 7;
    }

    public boolean isFastSegment() {
        return averageSpeed >= 40.0 && !isUrbanArea;
    }

    public boolean isSlowSegment() {
        return averageSpeed <= 15.0 || (isUrbanArea && hasTrafficLights && trafficComplexity >= 8);
    }

    public double getOptimalSpeedForPeriod(TimePeriod period) {
        EstimatedDuration timeForPeriod = getTimeForPeriod(period);
        if (timeForPeriod.getHours() <= 0) return 0.0;
        return distance.toKilometers() / timeForPeriod.getHours();
    }


    private static double calculateAverageSpeed(Distance distance, EstimatedDuration time) {
        if (time.getHours() <= 0) {
            throw new BusinessRuleViolationException(
                    "INVALID_TIME_FOR_SPEED",
                    "Time must be positive for speed calculation");
        }
        return distance.toKilometers() / time.getHours();
    }

    private static EstimatedDuration adjustForTrafficConditions(EstimatedDuration baseTime,
                                                                int trafficComplexity,
                                                                boolean hasTrafficLights,
                                                                boolean isUrbanArea) {
        double multiplier = 1.0;

        // Влияние сложности трафика (каждый уровень +10%)
        multiplier += (trafficComplexity - 1) * 0.1;

        // Светофоры добавляют 15% времени
        if (hasTrafficLights) {
            multiplier += 0.15;
        }

        // Городская зона добавляет 10% времени
        if (isUrbanArea) {
            multiplier += 0.1;
        }

        return baseTime.multiply(multiplier);
    }

    private static EstimatedDuration calculateRushHourTime(EstimatedDuration baseTime,
                                                           int trafficComplexity,
                                                           boolean isUrbanArea) {
        double rushMultiplier = 1.2;

        if (isUrbanArea) {
            rushMultiplier += 0.3;
        }

        if (trafficComplexity >= 7) {
            rushMultiplier += 0.2;
        }

        return baseTime.multiply(rushMultiplier);
    }

    private static EstimatedDuration calculateNightTime(EstimatedDuration baseTime, boolean isUrbanArea) {
        // Ночью меньше трафика: город -15%, пригород -25%
        double nightMultiplier = isUrbanArea ? 0.85 : 0.75;
        return baseTime.multiply(nightMultiplier);
    }

    private static StopId validateStopId(StopId stopId, String fieldName) {
        if (stopId == null) {
            throw new BusinessRuleViolationException(
                    "INVALID_STOP_ID",
                    fieldName + " cannot be null");
        }
        return stopId;
    }

    private static Distance validateDistance(Distance distance) {
        if (distance == null) {
            throw new BusinessRuleViolationException(
                    "INVALID_DISTANCE",
                    "Distance cannot be null");
        }
        if (distance.getMeters() < MIN_SEGMENT_DISTANCE_M) {
            throw new BusinessRuleViolationException(
                    "DISTANCE_TOO_SHORT",
                    "Segment distance must be at least " + MIN_SEGMENT_DISTANCE_M + " meters");
        }
        if (distance.toKilometers() > MAX_SEGMENT_DISTANCE_KM) {
            throw new BusinessRuleViolationException(
                    "DISTANCE_TOO_LONG",
                    "Segment distance cannot exceed " + MAX_SEGMENT_DISTANCE_KM + " km");
        }
        return distance;
    }

    private static EstimatedDuration validateEstimatedTime(EstimatedDuration time) {
        if (time == null) {
            throw new BusinessRuleViolationException(
                    "INVALID_ESTIMATED_TIME",
                    "Estimated time cannot be null");
        }
        if (time.getSeconds() < MIN_SEGMENT_TIME_SECONDS) {
            throw new BusinessRuleViolationException(
                    "TIME_TOO_SHORT",
                    "Segment time must be at least " + MIN_SEGMENT_TIME_SECONDS + " seconds");
        }
        if (time.getHours() > MAX_SEGMENT_TIME_HOURS) {
            throw new BusinessRuleViolationException(
                    "TIME_TOO_LONG",
                    "Segment time cannot exceed " + MAX_SEGMENT_TIME_HOURS + " hours");
        }
        return time;
    }

    private static double validateAverageSpeed(double speed, Distance distance, EstimatedDuration time) {
        if (speed < MIN_SPEED_KMH || speed > MAX_SPEED_KMH) {
            throw new BusinessRuleViolationException(
                    "INVALID_AVERAGE_SPEED",
                    "Average speed must be between " + MIN_SPEED_KMH + " and " + MAX_SPEED_KMH + " km/h");
        }

        double calculatedSpeed = distance.toKilometers() / time.getHours();
        double tolerance = calculatedSpeed * 0.1; // 10% tolerance
        if (Math.abs(calculatedSpeed - speed) > tolerance) {
            throw new BusinessRuleViolationException(
                    "SPEED_MISMATCH",
                    "Speed mismatch - calculated: " + calculatedSpeed +
                            " km/h, provided: " + speed + " km/h"
            );
        }
        return speed;
    }

    private static void validateTrafficParameters(int trafficComplexity) {
        if (trafficComplexity < 1 || trafficComplexity > 10) {
            throw new BusinessRuleViolationException(
                    "INVALID_TRAFFIC_COMPLEXITY",
                    "Traffic complexity must be between 1 and 10");
        }
    }

    private void validateSegmentConstraints() {
        if (fromStopId.equals(toStopId)) {
            throw new BusinessRuleViolationException(
                    "SAME_STOP_SEGMENT",
                    "Segment cannot start and end at the same stop");
        }
    }


    @Getter
    public enum TimePeriod {
        RUSH_HOUR("Rush Hour", "07:00-09:00, 17:00-19:00"),
        OFF_PEAK("Off Peak", "09:00-17:00"),
        NIGHT("Night", "22:00-06:00"),
        WEEKEND("Weekend", "Saturday-Sunday");

        private final String displayName;
        private final String description;

        TimePeriod(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

    }



    @Override
    public String toString() {
        return String.format(
                "RouteSegment{id=%s, from=%s, to=%s, distance=%s, time=%s, speed=%.1f km/h, complexity=%d}",
                getId(), fromStopId, toStopId, distance, estimatedTime, averageSpeed, trafficComplexity);
    }

    public String getDetailedInfo() {
        return String.format(
                """
                        Segment %s: %s -> %s
                        Distance: %s, Base Time: %s (Speed: %.1f km/h)
                        Rush Hour: %s, Off Peak: %s, Night: %s
                        Traffic: %d/10, Lights: %s, Urban: %s, Difficulty: %d/10""",
                getId(), fromStopId, toStopId,
                distance, estimatedTime, averageSpeed,
                rushHourTime, offPeakTime, nightTime,
                trafficComplexity, hasTrafficLights ? "Yes" : "No",
                isUrbanArea ? "Yes" : "No", getDifficultyScore()
        );
    }
}

