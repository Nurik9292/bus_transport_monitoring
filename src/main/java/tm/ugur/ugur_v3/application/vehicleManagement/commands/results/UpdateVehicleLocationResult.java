package tm.ugur.ugur_v3.application.vehicleManagement.commands.results;

import lombok.Builder;
import tm.ugur.ugur_v3.application.shared.commands.CommandResult;
import tm.ugur.ugur_v3.domain.shared.valueobjects.GeoCoordinate;
import tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects.VehicleId;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Builder
public record UpdateVehicleLocationResult(
        VehicleId vehicleId,
        boolean success,
        GeoCoordinate previousLocation,
        GeoCoordinate newLocation,
        Double distanceTraveledMeters,
        Double speedKmh,
        Double bearingDegrees,
        String errorMessage,
        String errorCode,
        Duration processingTime,
        Instant timestamp,
        boolean isSignificantMovement,
        boolean triggerredRouteEvents,
        Optional<String> routeId,
        Map<String, Object> gpsMetadata
) implements CommandResult {


    public static UpdateVehicleLocationResult success(VehicleId vehicleId,
                                                      GeoCoordinate previousLocation,
                                                      GeoCoordinate newLocation,
                                                      Duration processingTime) {
        return UpdateVehicleLocationResult.builder()
                .vehicleId(vehicleId)
                .success(true)
                .previousLocation(previousLocation)
                .newLocation(newLocation)
                .distanceTraveledMeters(calculateDistance(previousLocation, newLocation))
                .speedKmh(null)
                .bearingDegrees(null)
                .errorMessage(null)
                .errorCode(null)
                .processingTime(processingTime)
                .timestamp(Instant.now())
                .isSignificantMovement(isSignificantMovement(previousLocation, newLocation))
                .triggerredRouteEvents(false)
                .routeId(Optional.empty())
                .gpsMetadata(Map.of(
                        "isSuccessful", true,
                        "hasMovement", previousLocation != null,
                        "accuracy", newLocation.getAccuracy()
                ))
                .build();
    }

    public static UpdateVehicleLocationResult successWithMovement(VehicleId vehicleId,
                                                                  GeoCoordinate previousLocation,
                                                                  GeoCoordinate newLocation,
                                                                  Double speedKmh,
                                                                  Double bearingDegrees,
                                                                  Duration processingTime) {
        double distanceMeters = calculateDistance(previousLocation, newLocation);
        boolean isSignificant = isSignificantMovement(previousLocation, newLocation);

        return UpdateVehicleLocationResult.builder()
                .vehicleId(vehicleId)
                .success(true)
                .previousLocation(previousLocation)
                .newLocation(newLocation)
                .distanceTraveledMeters(distanceMeters)
                .speedKmh(speedKmh)
                .bearingDegrees(bearingDegrees)
                .errorMessage(null)
                .errorCode(null)
                .processingTime(processingTime)
                .timestamp(Instant.now())
                .isSignificantMovement(isSignificant)
                .triggerredRouteEvents(false)
                .routeId(Optional.empty())
                .gpsMetadata(Map.of(
                        "isSuccessful", true,
                        "hasMovement", true,
                        "distanceMeters", distanceMeters,
                        "speedKmh", speedKmh != null ? speedKmh : 0.0,
                        "bearingDegrees", bearingDegrees != null ? bearingDegrees : 0.0,
                        "accuracy", newLocation.getAccuracy(),
                        "isSignificantMovement", isSignificant
                ))
                .build();
    }

    public static UpdateVehicleLocationResult successWithRouteEvents(VehicleId vehicleId,
                                                                     GeoCoordinate previousLocation,
                                                                     GeoCoordinate newLocation,
                                                                     String routeId,
                                                                     Duration processingTime) {
        return UpdateVehicleLocationResult.builder()
                .vehicleId(vehicleId)
                .success(true)
                .previousLocation(previousLocation)
                .newLocation(newLocation)
                .distanceTraveledMeters(calculateDistance(previousLocation, newLocation))
                .speedKmh(null)
                .bearingDegrees(null)
                .errorMessage(null)
                .errorCode(null)
                .processingTime(processingTime)
                .timestamp(Instant.now())
                .isSignificantMovement(true)
                .triggerredRouteEvents(true)
                .routeId(Optional.of(routeId))
                .gpsMetadata(Map.of(
                        "isSuccessful", true,
                        "hasRouteEvents", true,
                        "routeId", routeId,
                        "accuracy", newLocation.getAccuracy()
                ))
                .build();
    }

    public static UpdateVehicleLocationResult firstLocationUpdate(VehicleId vehicleId,
                                                                  GeoCoordinate newLocation,
                                                                  Duration processingTime) {
        return UpdateVehicleLocationResult.builder()
                .vehicleId(vehicleId)
                .success(true)
                .previousLocation(null)
                .newLocation(newLocation)
                .distanceTraveledMeters(0.0)
                .speedKmh(0.0)
                .bearingDegrees(null)
                .errorMessage(null)
                .errorCode(null)
                .processingTime(processingTime)
                .timestamp(Instant.now())
                .isSignificantMovement(true)
                .triggerredRouteEvents(false)
                .routeId(Optional.empty())
                .gpsMetadata(Map.of(
                        "isSuccessful", true,
                        "isFirstLocation", true,
                        "accuracy", newLocation.getAccuracy()
                ))
                .build();
    }


    public static UpdateVehicleLocationResult failure(VehicleId vehicleId, String errorMessage) {
        return UpdateVehicleLocationResult.builder()
                .vehicleId(vehicleId)
                .success(false)
                .previousLocation(null)
                .newLocation(null)
                .distanceTraveledMeters(null)
                .speedKmh(null)
                .bearingDegrees(null)
                .errorMessage(errorMessage)
                .errorCode("LOCATION_UPDATE_FAILED")
                .processingTime(Duration.ZERO)
                .timestamp(Instant.now())
                .isSignificantMovement(false)
                .triggerredRouteEvents(false)
                .routeId(Optional.empty())
                .gpsMetadata(Map.of(
                        "isSuccessful", false,
                        "error", errorMessage
                ))
                .build();
    }

    public static UpdateVehicleLocationResult invalidCoordinates(VehicleId vehicleId,
                                                                 double latitude,
                                                                 double longitude,
                                                                 String reason) {
        return UpdateVehicleLocationResult.builder()
                .vehicleId(vehicleId)
                .success(false)
                .previousLocation(null)
                .newLocation(null)
                .distanceTraveledMeters(null)
                .speedKmh(null)
                .bearingDegrees(null)
                .errorMessage(String.format("Invalid coordinates: lat=%.6f, lng=%.6f (%s)",
                        latitude, longitude, reason))
                .errorCode("INVALID_COORDINATES")
                .processingTime(Duration.ZERO)
                .timestamp(Instant.now())
                .isSignificantMovement(false)
                .triggerredRouteEvents(false)
                .routeId(Optional.empty())
                .gpsMetadata(Map.of(
                        "isSuccessful", false,
                        "invalidLatitude", latitude,
                        "invalidLongitude", longitude,
                        "validationFailure", reason
                ))
                .build();
    }

    public static UpdateVehicleLocationResult accuracyTooLow(VehicleId vehicleId,
                                                             GeoCoordinate location,
                                                             double accuracy,
                                                             double minRequired) {
        return UpdateVehicleLocationResult.builder()
                .vehicleId(vehicleId)
                .success(false)
                .previousLocation(null)
                .newLocation(location)
                .distanceTraveledMeters(null)
                .speedKmh(null)
                .bearingDegrees(null)
                .errorMessage(String.format("GPS accuracy too low: %.1fm (minimum: %.1fm)",
                        accuracy, minRequired))
                .errorCode("ACCURACY_TOO_LOW")
                .processingTime(Duration.ZERO)
                .timestamp(Instant.now())
                .isSignificantMovement(false)
                .triggerredRouteEvents(false)
                .routeId(Optional.empty())
                .gpsMetadata(Map.of(
                        "isSuccessful", false,
                        "actualAccuracy", accuracy,
                        "requiredAccuracy", minRequired,
                        "accuracyRejected", true
                ))
                .build();
    }

    public static UpdateVehicleLocationResult vehicleNotTrackable(VehicleId vehicleId, String status) {
        return UpdateVehicleLocationResult.builder()
                .vehicleId(vehicleId)
                .success(false)
                .previousLocation(null)
                .newLocation(null)
                .distanceTraveledMeters(null)
                .speedKmh(null)
                .bearingDegrees(null)
                .errorMessage(String.format("Vehicle not trackable in status: %s", status))
                .errorCode("VEHICLE_NOT_TRACKABLE")
                .processingTime(Duration.ZERO)
                .timestamp(Instant.now())
                .isSignificantMovement(false)
                .triggerredRouteEvents(false)
                .routeId(Optional.empty())
                .gpsMetadata(Map.of(
                        "isSuccessful", false,
                        "vehicleStatus", status,
                        "trackingDisabled", true
                ))
                .build();
    }

    public static UpdateVehicleLocationResult jumpTooLarge(VehicleId vehicleId,
                                                           GeoCoordinate previousLocation,
                                                           GeoCoordinate newLocation,
                                                           double distanceKm) {
        return UpdateVehicleLocationResult.builder()
                .vehicleId(vehicleId)
                .success(false)
                .previousLocation(previousLocation)
                .newLocation(newLocation)
                .distanceTraveledMeters(distanceKm * 1000)
                .speedKmh(null)
                .bearingDegrees(null)
                .errorMessage(String.format("Location jump too large: %.2f km", distanceKm))
                .errorCode("JUMP_TOO_LARGE")
                .processingTime(Duration.ZERO)
                .timestamp(Instant.now())
                .isSignificantMovement(false)
                .triggerredRouteEvents(false)
                .routeId(Optional.empty())
                .gpsMetadata(Map.of(
                        "isSuccessful", false,
                        "jumpDistanceKm", distanceKm,
                        "previousLat", previousLocation.getLatitude(),
                        "previousLng", previousLocation.getLongitude(),
                        "newLat", newLocation.getLatitude(),
                        "newLng", newLocation.getLongitude(),
                        "jumpRejected", true
                ))
                .build();
    }

    public boolean isFailure() {
        return !success;
    }

    public boolean hasMovement() {
        return previousLocation != null && newLocation != null && isSignificantMovement;
    }

    public boolean hasSpeed() {
        return speedKmh != null && speedKmh > 0;
    }

    public boolean hasBearing() {
        return bearingDegrees != null;
    }

    public boolean isVehicleMoving() {
        return hasSpeed() && speedKmh > 5.0;
    }

    public boolean isVehicleStationary() {
        return !hasSpeed() || speedKmh <= 5.0;
    }

    public boolean isFirstLocationUpdate() {
        return success && previousLocation == null && newLocation != null;
    }

    public double getDistanceTraveledKm() {
        return distanceTraveledMeters != null ? distanceTraveledMeters / 1000.0 : 0.0;
    }

    public String getMovementSummary() {
        if (!success) {
            return "Location update failed: " + (errorMessage != null ? errorMessage : "unknown error");
        }

        if (isFirstLocationUpdate()) {
            return String.format("First location set: %s", newLocation);
        }

        if (!hasMovement()) {
            return "No significant movement detected";
        }

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Moved %.0f meters", distanceTraveledMeters));

        if (hasSpeed()) {
            summary.append(String.format(" at %.1f km/h", speedKmh));
        }

        if (hasBearing()) {
            summary.append(String.format(" heading %.0f°", bearingDegrees));
        }

        return summary.toString();
    }

    public String getLocationSummary() {
        if (!success || newLocation == null) {
            return "No location data";
        }

        return String.format("Location: %s (accuracy: %.1fm)",
                newLocation.toString(), newLocation.getAccuracy());
    }


    @Override
    public boolean isSuccessful() {
        return success;
    }

    @Override
    public Duration getProcessingTime() {
        return processingTime;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = Map.of(
                "resultType", "UpdateVehicleLocationResult",
                "isSuccessful", success,
                "hasMovement", hasMovement(),
                "isSignificantMovement", isSignificantMovement,
                "hasSpeed", hasSpeed(),
                "hasBearing", hasBearing(),
                "isFirstUpdate", isFirstLocationUpdate(),
                "triggeredRouteEvents", triggerredRouteEvents,
                "processingTime", processingTime.toMillis(),
                "timestamp", timestamp.toString()
        );


        if (!gpsMetadata.isEmpty()) {
            Map<String, Object> combined = new java.util.HashMap<>(metadata);
            combined.putAll(gpsMetadata);
            return Map.copyOf(combined);
        }

        return metadata;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public boolean isCritical() {
        return !success && (
                "JUMP_TOO_LARGE".equals(errorCode) ||
                        "VEHICLE_NOT_TRACKABLE".equals(errorCode) ||
                        hasLowAccuracy()
        );
    }

    @Override
    public boolean requiresNotification() {
        return isCritical() || triggerredRouteEvents || isFirstLocationUpdate();
    }


    private static double calculateDistance(GeoCoordinate from, GeoCoordinate to) {
        if (from == null || to == null) {
            return 0.0;
        }


        double lat1Rad = Math.toRadians(from.getLatitude());
        double lat2Rad = Math.toRadians(to.getLatitude());
        double deltaLatRad = Math.toRadians(to.getLatitude() - from.getLatitude());
        double deltaLngRad = Math.toRadians(to.getLongitude() - from.getLongitude());

        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLngRad / 2) * Math.sin(deltaLngRad / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return 6371000 * c;
    }

    private static boolean isSignificantMovement(GeoCoordinate from, GeoCoordinate to) {
        if (from == null || to == null) {
            return to != null;
        }

        double distanceMeters = calculateDistance(from, to);
        return distanceMeters >= 10.0;
    }


    public boolean isSlowProcessing() {
        return processingTime.toMillis() > 50;
    }

    public boolean isFastProcessing() {
        return processingTime.toMillis() <= 25;
    }

    @Override
    public PerformanceCategory getPerformanceCategory() {
        long millis = processingTime.toMillis();
        if (millis <= 25) return PerformanceCategory.FAST;
        if (millis <= 50) return PerformanceCategory.NORMAL;
        if (millis <= 200) return PerformanceCategory.SLOW;
        return PerformanceCategory.VERY_SLOW;
    }


    public boolean hasHighAccuracy() {
        return newLocation != null && newLocation.getAccuracy() <= 10.0;
    }

    public boolean hasLowAccuracy() {
        return newLocation != null && newLocation.getAccuracy() > 50.0;
    }

    public String getGpsQuality() {
        if (newLocation == null) return "NO_GPS";

        double accuracy = newLocation.getAccuracy();
        if (accuracy <= 5.0) return "EXCELLENT";
        if (accuracy <= 10.0) return "GOOD";
        if (accuracy <= 25.0) return "ACCEPTABLE";
        if (accuracy <= 50.0) return "POOR";
        return "VERY_POOR";
    }

}