package tm.ugur.ugur_v3.domain.routeManagement.enums;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import java.util.Set;

@Getter
public enum RouteType {


    LOCAL_BUS("Local Bus", "Standard city bus service", 30, 60, true, true),
    EXPRESS("Express", "Express service with limited stops", 50, 80, true, false),
    EXPRESS_BUS("Express Bus", "Limited stops express service", 50, 80, true, false),
    BRT("Bus Rapid Transit", "Dedicated busway service", 40, 70, true, true),


    METRO("Metro", "Underground rail service", 35, 80, false, true),
    LIGHT_RAIL("Light Rail", "Surface light rail", 25, 60, false, true),
    TRAM("Tram", "Street-running tram", 20, 50, false, true),


    TROLLEYBUS("Trolleybus", "Electric trolleybus service", 25, 55, true, true),
    SHUTTLE("Shuttle", "Short-distance shuttle", 15, 40, true, false),
    FEEDER("Feeder", "Connection to main transit", 20, 45, true, false),


    COMMUTER_RAIL("Commuter Rail", "Regional rail service", 60, 120, false, false),
    INTERCITY_BUS("Intercity Bus", "Long-distance bus service", 80, 120, true, false),


    AIRPORT_SHUTTLE("Airport Shuttle", "Airport connection service", 40, 80, true, false),
    NIGHT_SERVICE("Night Service", "Late night/early morning service", 25, 50, true, true),
    SCHOOL_BUS("School Bus", "Educational institution service", 20, 40, true, false);

    private final String displayName;
    private final String description;
    private final int typicalSpeedKmh;
    private final int maxSpeedKmh;
    private final boolean allowsStandingPassengers;
    private final boolean operatesInMixedTraffic;

    RouteType(String displayName, String description, int typicalSpeedKmh, int maxSpeedKmh,
              boolean allowsStandingPassengers, boolean operatesInMixedTraffic) {
        this.displayName = displayName;
        this.description = description;
        this.typicalSpeedKmh = typicalSpeedKmh;
        this.maxSpeedKmh = maxSpeedKmh;
        this.allowsStandingPassengers = allowsStandingPassengers;
        this.operatesInMixedTraffic = operatesInMixedTraffic;
    }


    public boolean isRailBased() {
        return this == METRO || this == LIGHT_RAIL || this == TRAM || this == COMMUTER_RAIL;
    }

    public boolean isBusBased() {
        return this == LOCAL_BUS || this == EXPRESS_BUS || this == BRT ||
                this == TROLLEYBUS || this == SHUTTLE || this == FEEDER ||
                this == INTERCITY_BUS || this == AIRPORT_SHUTTLE || this == SCHOOL_BUS;
    }

    public boolean isHighFrequency() {
        return this == LOCAL_BUS || this == METRO || this == BRT || this == LIGHT_RAIL;
    }

    public boolean isExpressService() {
        return this == EXPRESS || this == EXPRESS_BUS || this == BRT || this == COMMUTER_RAIL || this == INTERCITY_BUS;
    }

    public boolean requiresDedicatedInfrastructure() {
        return this == METRO || this == BRT || this == LIGHT_RAIL || this == COMMUTER_RAIL;
    }

    public boolean isAccessible() {

        return this != SCHOOL_BUS;
    }


    public int getMinimumHeadwayMinutes() {
        return switch (this) {
            case METRO, BRT -> 2;
            case LOCAL_BUS, LIGHT_RAIL, TRAM -> 5;
            case EXPRESS_BUS, TROLLEYBUS, EXPRESS -> 10;
            case SHUTTLE, FEEDER -> 15;
            case COMMUTER_RAIL, NIGHT_SERVICE -> 30;
            case INTERCITY_BUS, AIRPORT_SHUTTLE, SCHOOL_BUS -> 60;
        };
    }


    public int getMaximumHeadwayMinutes() {
        return switch (this) {
            case METRO -> 10;
            case BRT, LOCAL_BUS, AIRPORT_SHUTTLE, TROLLEYBUS, SHUTTLE, FEEDER -> 30;
            case LIGHT_RAIL, TRAM, EXPRESS_BUS, EXPRESS, NIGHT_SERVICE -> 60;
            case COMMUTER_RAIL -> 120;
            case INTERCITY_BUS -> 240;
            case SCHOOL_BUS -> 480;

        };
    }

    public int getTypicalVehicleCapacity() {
        return switch (this) {
            case METRO -> 150;
            case BRT -> 120;
            case LOCAL_BUS, EXPRESS_BUS, EXPRESS -> 80;
            case LIGHT_RAIL -> 200;
            case TRAM -> 100;
            case TROLLEYBUS -> 90;
            case SHUTTLE -> 25;
            case FEEDER -> 40;
            case COMMUTER_RAIL -> 300;
            case INTERCITY_BUS -> 50;
            case AIRPORT_SHUTTLE -> 30;
            case NIGHT_SERVICE -> 60;
            case SCHOOL_BUS -> 70;
        };
    }

    public void validateSpeed(double speedKmh) {
        if (speedKmh > maxSpeedKmh) {
            throw new BusinessRuleViolationException(
                    "ROUTE_TYPE_SPEED",
                    String.format("Speed %.1f km/h exceeds maximum for %s (%.1f km/h)",
                            speedKmh, this.displayName, (double) maxSpeedKmh)
            );
        }
        if (speedKmh < (typicalSpeedKmh * 0.3)) {
            throw new BusinessRuleViolationException(
                    "ROUTE_TYPE_SPEED",
                    String.format("Speed %.1f km/h too low for %s (minimum %.1f km/h)",
                            speedKmh, this.displayName, typicalSpeedKmh * 0.3)
            );
        }
    }

    public Set<String> getCompatibleVehicleTypes() {
        return switch (this) {
            case LOCAL_BUS, EXPRESS_BUS, NIGHT_SERVICE -> Set.of("STANDARD_BUS", "ARTICULATED_BUS", "DOUBLE_DECKER");
            case BRT -> Set.of("BRT_VEHICLE", "ARTICULATED_BUS");
            case METRO -> Set.of("METRO_TRAIN");
            case LIGHT_RAIL -> Set.of("LIGHT_RAIL_VEHICLE");
            case TRAM -> Set.of("TRAM");
            case TROLLEYBUS -> Set.of("TROLLEYBUS");
            case SHUTTLE -> Set.of("MINIBUS", "SMALL_BUS");
            case FEEDER -> Set.of("STANDARD_BUS", "MINIBUS");
            case COMMUTER_RAIL -> Set.of("COMMUTER_TRAIN");
            case INTERCITY_BUS -> Set.of("COACH", "INTERCITY_BUS");
            case AIRPORT_SHUTTLE -> Set.of("SHUTTLE_BUS", "MINIBUS");
            case SCHOOL_BUS -> Set.of("SCHOOL_BUS");
            case EXPRESS -> Set.of("EXPRESS_BUS");
        };
    }

    public static Set<RouteType> getHighCapacityTypes() {
        return Set.of(METRO, BRT, LIGHT_RAIL, COMMUTER_RAIL);
    }

    public static Set<RouteType> getUrbanTypes() {
        return Set.of(LOCAL_BUS, EXPRESS_BUS, BRT, METRO, LIGHT_RAIL, TRAM, TROLLEYBUS, SHUTTLE);
    }

    public static Set<RouteType> getRegionalTypes() {
        return Set.of(COMMUTER_RAIL, INTERCITY_BUS);
    }

    public static Set<RouteType> getSpecialPurposeTypes() {
        return Set.of(AIRPORT_SHUTTLE, NIGHT_SERVICE, SCHOOL_BUS, FEEDER);
    }
}