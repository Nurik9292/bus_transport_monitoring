package tm.ugur.ugur_v3.domain.routeManagement.enums;

import lombok.Getter;


@Getter
public enum RouteDirection {
    FORWARD("Прямое", "A → B", "→"),
    BACKWARD("Обратное", "B → A", "←"),
    CIRCULAR_CLOCKWISE("Кольцевое по часовой", "Движение по часовой стрелке", "↻"),
    CIRCULAR_COUNTERCLOCKWISE("Кольцевое против часовой", "Движение против часовой стрелки", "↺"),
    BIDIRECTIONAL("Двунаправленное", "A ⟷ B", "⟷");

    private final String displayName;
    private final String description;
    private final String symbol;

    RouteDirection(String displayName, String description, String symbol) {
        this.displayName = displayName;
        this.description = description;
        this.symbol = symbol;
    }

    public boolean isCircular() {
        return this == CIRCULAR_CLOCKWISE || this == CIRCULAR_COUNTERCLOCKWISE;
    }

    public boolean isLinear() {
        return this == FORWARD || this == BACKWARD || this == BIDIRECTIONAL;
    }

    public boolean hasReturnDirection() {
        return this == BIDIRECTIONAL;
    }

    public RouteDirection getOpposite() {
        return switch (this) {
            case FORWARD -> BACKWARD;
            case BACKWARD -> FORWARD;
            case CIRCULAR_CLOCKWISE -> CIRCULAR_COUNTERCLOCKWISE;
            case CIRCULAR_COUNTERCLOCKWISE -> CIRCULAR_CLOCKWISE;
            case BIDIRECTIONAL -> BIDIRECTIONAL;
        };
    }

    public boolean requiresReverseRoute() {
        return this == FORWARD || this == BACKWARD;
    }

    @Override
    public String toString() {
        return displayName + " " + symbol;
    }
}