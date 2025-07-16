package tm.ugur.ugur_v3.domain.routeManagement.enums;

import lombok.Getter;

import java.util.Set;

@Getter
public enum RouteDirection {

    FORWARD("Forward", "Normal direction", "A→B", 1),
    BACKWARD("Backward", "Reverse direction", "B→A", -1),
    BIDIRECTIONAL("Bidirectional", "Both directions", "A↔B", 0),
    CIRCULAR("Circular", "Circular route", "A→B→C→A", 1);

    private final String displayName;
    private final String description;
    private final String symbol;
    private final int directionMultiplier;

    RouteDirection(String displayName, String description, String symbol, int directionMultiplier) {
        this.displayName = displayName;
        this.description = description;
        this.symbol = symbol;
        this.directionMultiplier = directionMultiplier;
    }

    public boolean isReversible() {
        return this == BIDIRECTIONAL || this == CIRCULAR;
    }

    public boolean requiresReturnRoute() {
        return this == FORWARD || this == BACKWARD;
    }

    public boolean isLoop() {
        return this == CIRCULAR;
    }

    public RouteDirection getOpposite() {
        return switch (this) {
            case FORWARD -> BACKWARD;
            case BACKWARD -> FORWARD;
            case BIDIRECTIONAL -> BIDIRECTIONAL;
            case CIRCULAR -> CIRCULAR;
        };
    }

    public static Set<RouteDirection> getCompatibleDirections(RouteDirection primary) {
        return switch (primary) {
            case FORWARD -> Set.of(FORWARD, BIDIRECTIONAL);
            case BACKWARD -> Set.of(BACKWARD, BIDIRECTIONAL);
            case BIDIRECTIONAL -> Set.of(FORWARD, BACKWARD, BIDIRECTIONAL);
            case CIRCULAR -> Set.of(CIRCULAR);
        };
    }
}