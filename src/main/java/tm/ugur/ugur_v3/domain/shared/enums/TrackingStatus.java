package tm.ugur.ugur_v3.domain.shared.enums;

public enum TrackingStatus {

    ACTIVE("Активный"),
    INACTIVE("Неактивный"),
    LOST_SIGNAL("Потеря сигнала"),
    MAINTENANCE("Обслуживание"),
    OUT_OF_SERVICE("Вне службы");

    private final String displayName;

    TrackingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTrackable() {
        return this == ACTIVE;
    }
}
