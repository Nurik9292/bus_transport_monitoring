package tm.ugur.ugur_v3.domain.vehicleManagement.enums;

import lombok.Getter;

@Getter
public enum VehicleType {
    BUS("Автобус", 40, 80),
    TROLLEY("Троллейбус", 35, 70),
    MINIBUS("Маршрутка", 15, 20),
    TRAM("Трамвай", 50, 120);

    private final String displayName;
    private final int typicalSeatedCapacity;
    private final int typicalTotalCapacity;

    VehicleType(String displayName, int seatedCapacity, int totalCapacity) {
        this.displayName = displayName;
        this.typicalSeatedCapacity = seatedCapacity;
        this.typicalTotalCapacity = totalCapacity;
    }

    public boolean isSuitableForRoute(int expectedPassengerCount) {
        return expectedPassengerCount <= typicalTotalCapacity;
    }
}

