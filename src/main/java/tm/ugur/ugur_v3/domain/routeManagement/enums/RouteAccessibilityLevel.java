package tm.ugur.ugur_v3.domain.routeManagement.enums;

import lombok.Getter;

@Getter
public enum RouteAccessibilityLevel {
    FULLY_ACCESSIBLE("Полностью доступный", "Все остановки и транспорт доступны", 100),
    MOSTLY_ACCESSIBLE("В основном доступный", "Большинство остановок доступны", 80),
    PARTIALLY_ACCESSIBLE("Частично доступный", "Некоторые остановки доступны", 50),
    LIMITED_ACCESSIBILITY("Ограниченная доступность", "Минимальная доступность", 25),
    NOT_ACCESSIBLE("Недоступный", "Недоступен для людей с ограниченными возможностями", 0);

    private final String displayName;
    private final String description;
    private final int accessibilityScore;

    RouteAccessibilityLevel(String displayName, String description, int accessibilityScore) {
        this.displayName = displayName;
        this.description = description;
        this.accessibilityScore = accessibilityScore;
    }

    public static RouteAccessibilityLevel fromScore(int score) {
        if (score >= 95) return FULLY_ACCESSIBLE;
        if (score >= 75) return MOSTLY_ACCESSIBLE;
        if (score >= 45) return PARTIALLY_ACCESSIBLE;
        if (score >= 20) return LIMITED_ACCESSIBILITY;
        return NOT_ACCESSIBLE;
    }

    public boolean meetsStandards() {
        return accessibilityScore >= 80;
    }

    public boolean requiresImprovement() {
        return accessibilityScore < 50;
    }

    public String getRequiredFeatures() {
        return switch (this) {
            case FULLY_ACCESSIBLE -> "Все виды доступности обеспечены";
            case MOSTLY_ACCESSIBLE -> "Незначительные улучшения";
            case PARTIALLY_ACCESSIBLE -> "Нужны пандусы, аудиообъявления";
            case LIMITED_ACCESSIBILITY -> "Требуется капитальная модернизация";
            case NOT_ACCESSIBLE -> "Полная реконструкция для доступности";
        };
    }

    @Override
    public String toString() {
        return displayName + " (" + accessibilityScore + "%)";
    }
}