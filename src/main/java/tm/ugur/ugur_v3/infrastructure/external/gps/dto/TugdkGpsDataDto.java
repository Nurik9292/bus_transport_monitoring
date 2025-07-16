
package tm.ugur.ugur_v3.infrastructure.external.gps.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

public record TugdkGpsDataDto(
        @JsonProperty("id") long id,
        @JsonProperty("deviceId") int deviceId,
        @JsonProperty("protocol") String protocol,
        @JsonProperty("serverTime") String serverTime,
        @JsonProperty("deviceTime") String deviceTime,
        @JsonProperty("fixTime") String fixTime,
        @JsonProperty("outdated") boolean outdated,
        @JsonProperty("valid") boolean valid,
        @JsonProperty("latitude") double latitude,
        @JsonProperty("longitude") double longitude,
        @JsonProperty("altitude") double altitude,
        @JsonProperty("speed") double speed,
        @JsonProperty("course") double course,
        @JsonProperty("address") String address,
        @JsonProperty("accuracy") double accuracy,
        @JsonProperty("network") String network,
        @JsonProperty("geofenceIds") Object geofenceIds,
        @JsonProperty("attributes") TugdkAttributes attributes
) {

    public record TugdkAttributes(
            @JsonProperty("sat") int sat,
            @JsonProperty("hdop") double hdop,
            @JsonProperty("event") int event,
            @JsonProperty("in1") int in1,
            @JsonProperty("in2") int in2,
            @JsonProperty("in4") int in4,
            @JsonProperty("io27") int io27,
            @JsonProperty("io28") int io28,
            @JsonProperty("deviceTemp") int deviceTemp,
            @JsonProperty("motion") boolean motion,
            @JsonProperty("power") double power,
            @JsonProperty("battery") double battery,
            @JsonProperty("adc1") int adc1,
            @JsonProperty("adc2") int adc2,
            @JsonProperty("odometer") long odometer,
            @JsonProperty("operator") int operator,
            @JsonProperty("distance") double distance,
            @JsonProperty("totalDistance") double totalDistance,
            @JsonProperty("stopTime") String stopTime,
            @JsonProperty("name") String name,
            @JsonProperty("uniqueId") String uniqueId
    ) {

        public String getVehicleIdentifier() {
            return name != null ? name.trim() : uniqueId;
        }

        public boolean isMoving() {
            return motion;
        }

        public GpsSignalQuality getSignalQuality() {
            if (sat >= 8) return GpsSignalQuality.EXCELLENT;
            if (sat >= 6) return GpsSignalQuality.GOOD;
            if (sat >= 4) return GpsSignalQuality.FAIR;
            return GpsSignalQuality.POOR;
        }

        public boolean hasSufficientPower() {
            return power >= 12.0;
        }

        public DeviceHealthStatus getDeviceHealth() {
            if (!hasSufficientPower()) return DeviceHealthStatus.POWER_LOW;
            if (deviceTemp > 60) return DeviceHealthStatus.OVERHEATING;
            if (sat < 4) return DeviceHealthStatus.POOR_SIGNAL;
            return DeviceHealthStatus.HEALTHY;
        }

        public Map<String, Object> getDeviceMetadata() {
            return Map.of(
                    "satellites", sat,
                    "hdop", hdop,
                    "temperature", deviceTemp,
                    "power", power,
                    "battery", battery,
                    "odometer", odometer,
                    "operator", operator,
                    "signalQuality", getSignalQuality().name(),
                    "deviceHealth", getDeviceHealth().name()
            );
        }
    }

    public boolean isValidGpsData() {
        return valid &&
                !outdated &&
                latitude != 0.0 &&
                longitude != 0.0 &&
                attributes.sat >= 4 &&
                attributes.hdop <= 5.0;
    }

    public boolean isRecentFix() {
        try {
            Instant fixInstant = Instant.parse(fixTime);
            Instant now = Instant.now();
            return fixInstant.isAfter(now.minusSeconds(300)); // 5 minutes
        } catch (Exception e) {
            return false;
        }
    }

    public GpsAccuracyLevel getAccuracyLevel() {
        if (attributes.hdop <= 1.0 && attributes.sat >= 8) return GpsAccuracyLevel.HIGH;
        if (attributes.hdop <= 2.0 && attributes.sat >= 6) return GpsAccuracyLevel.MEDIUM;
        if (attributes.hdop <= 5.0 && attributes.sat >= 4) return GpsAccuracyLevel.LOW;
        return GpsAccuracyLevel.UNRELIABLE;
    }

    public String getVehicleIdentifier() {
        return attributes.getVehicleIdentifier();
    }

    public Map<String, Object> getGpsMetadata() {
        return Map.of(
                "deviceId", deviceId,
                "protocol", protocol,
                "serverTime", serverTime,
                "deviceTime", deviceTime,
                "accuracyLevel", getAccuracyLevel().name(),
                "isRecentFix", isRecentFix(),
                "deviceMetadata", attributes.getDeviceMetadata()
        );
    }

    @Getter
    public enum GpsSignalQuality {
        EXCELLENT("Excellent", 4),
        GOOD("Good", 3),
        FAIR("Fair", 2),
        POOR("Poor", 1);

        private final String description;
        private final int priority;

        GpsSignalQuality(String description, int priority) {
            this.description = description;
            this.priority = priority;
        }

    }

    @Getter
    public enum GpsAccuracyLevel {
        HIGH("High precision", 1.0),
        MEDIUM("Medium precision", 2.0),
        LOW("Low precision", 5.0),
        UNRELIABLE("Unreliable", 10.0);

        private final String description;
        private final double maxHdop;

        GpsAccuracyLevel(String description, double maxHdop) {
            this.description = description;
            this.maxHdop = maxHdop;
        }

    }

    @Getter
    public enum DeviceHealthStatus {
        HEALTHY("Device operating normally"),
        POWER_LOW("Low power warning"),
        OVERHEATING("Device overheating"),
        POOR_SIGNAL("Poor GPS signal");

        private final String description;

        DeviceHealthStatus(String description) {
            this.description = description;
        }

    }
}