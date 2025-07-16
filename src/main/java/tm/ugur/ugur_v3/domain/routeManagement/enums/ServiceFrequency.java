package tm.ugur.ugur_v3.domain.routeManagement.enums;

import lombok.Getter;

@Getter
public enum ServiceFrequency {

    VERY_HIGH("Very High", "Every 2-5 minutes", 2, 5),
    HIGH("High", "Every 5-10 minutes", 5, 10),
    MEDIUM("Medium", "Every 10-20 minutes", 10, 20),
    LOW("Low", "Every 20-30 minutes", 20, 30),
    VERY_LOW("Very Low", "Every 30-60 minutes", 30, 60),
    HOURLY("Hourly", "Every hour", 60, 60),
    LIMITED("Limited", "Few trips per day", 120, 480);

    private final String displayName;
    private final String description;
    private final int minHeadwayMinutes;
    private final int maxHeadwayMinutes;

    ServiceFrequency(String displayName, String description, int minHeadwayMinutes, int maxHeadwayMinutes) {
        this.displayName = displayName;
        this.description = description;
        this.minHeadwayMinutes = minHeadwayMinutes;
        this.maxHeadwayMinutes = maxHeadwayMinutes;
    }

    public boolean isHighFrequency() {
        return this == VERY_HIGH || this == HIGH;
    }

    public boolean isLowFrequency() {
        return this == VERY_LOW || this == HOURLY || this == LIMITED;
    }

    public boolean includesHeadway(int headwayMinutes) {
        return headwayMinutes >= minHeadwayMinutes && headwayMinutes <= maxHeadwayMinutes;
    }


    public static ServiceFrequency fromHeadway(int headwayMinutes) {
        for (ServiceFrequency freq : values()) {
            if (freq.includesHeadway(headwayMinutes)) {
                return freq;
            }
        }
        return LIMITED;
    }


    public double getTripsPerHour() {
        return 60.0 / ((minHeadwayMinutes + maxHeadwayMinutes) / 2.0);
    }


    public int getHourlyCapacity(int vehicleCapacity) {
        return (int) (getTripsPerHour() * vehicleCapacity);
    }
}