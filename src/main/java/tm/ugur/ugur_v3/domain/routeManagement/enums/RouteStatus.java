package tm.ugur.ugur_v3.domain.routeManagement.enums;

import lombok.Getter;

@Getter
public enum RouteStatus {
    DRAFT("Черновик", "Маршрут в разработке", false, false),
    PLANNING("Планирование", "Маршрут планируется", false, false),
    APPROVED("Утвержден", "Маршрут утвержден, готов к активации", false, true),
    ACTIVE("Активный", "Маршрут активно обслуживается", true, true),
    SUSPENDED("Приостановлен", "Маршрут временно приостановлен", false, true),
    MAINTENANCE("Техобслуживание", "Маршрут на техническом обслуживании", false, true),
    SEASONAL_INACTIVE("Сезонно неактивен", "Маршрут неактивен по сезонным причинам", false, true),
    ARCHIVED("Архивный", "Маршрут архивирован", false, false),
    CANCELLED("Отменен", "Маршрут отменен", false, false);

    private final String displayName;
    private final String description;
    private final boolean isOperational;
    private final boolean canBeActivated;

    RouteStatus(String displayName, String description, boolean isOperational, boolean canBeActivated) {
        this.displayName = displayName;
        this.description = description;
        this.isOperational = isOperational;
        this.canBeActivated = canBeActivated;
    }

    public boolean canTransitionTo(RouteStatus newStatus) {
        return switch (this) {
            case DRAFT -> newStatus == PLANNING || newStatus == CANCELLED;
            case PLANNING -> newStatus == APPROVED || newStatus == DRAFT || newStatus == CANCELLED;
            case APPROVED -> newStatus == ACTIVE || newStatus == PLANNING || newStatus == CANCELLED;
            case ACTIVE -> newStatus == SUSPENDED || newStatus == MAINTENANCE ||
                    newStatus == SEASONAL_INACTIVE || newStatus == ARCHIVED;
            case SUSPENDED -> newStatus == ACTIVE || newStatus == ARCHIVED || newStatus == CANCELLED;
            case MAINTENANCE -> newStatus == ACTIVE || newStatus == SUSPENDED;
            case SEASONAL_INACTIVE -> newStatus == ACTIVE || newStatus == ARCHIVED;
            case ARCHIVED -> newStatus == ACTIVE; // Возможно восстановление
            case CANCELLED -> false; // Финальный статус
        };
    }

    public boolean requiresApproval() {
        return this == ACTIVE || this == APPROVED;
    }

    public boolean allowsScheduleChanges() {
        return this == DRAFT || this == PLANNING || this == APPROVED;
    }

    @Override
    public String toString() {
        return displayName;
    }
}