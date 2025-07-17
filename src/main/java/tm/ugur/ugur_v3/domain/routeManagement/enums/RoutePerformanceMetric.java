package tm.ugur.ugur_v3.domain.routeManagement.enums;

import lombok.Getter;

@Getter
public enum RoutePerformanceMetric {
    ON_TIME_PERFORMANCE("Пунктуальность", "%", 85.0, 95.0, true),
    RIDERSHIP("Пассажиропоток", "чел/день", 100, 1000, true),
    LOAD_FACTOR("Коэффициент загрузки", "%", 60.0, 85.0, true),
    AVERAGE_SPEED("Средняя скорость", "км/ч", 15.0, 35.0, true),
    FUEL_EFFICIENCY("Топливная эффективность", "л/100км", 25.0, 35.0, false),
    CUSTOMER_SATISFACTION("Удовлетворенность", "балл", 3.5, 4.5, true),
    BREAKDOWN_FREQUENCY("Частота поломок", "раз/месяц", 0, 2, false),
    COST_PER_KM("Стоимость за км", "руб/км", 50.0, 100.0, false),
    REVENUE_PER_KM("Доход за км", "руб/км", 80.0, 150.0, true),
    ACCESSIBILITY_SCORE("Оценка доступности", "балл", 70, 100, true);

    private final String displayName;
    private final String unit;
    private final double targetMin;
    private final double targetMax;
    private final boolean higherIsBetter;

    RoutePerformanceMetric(String displayName, String unit, double targetMin,
                           double targetMax, boolean higherIsBetter) {
        this.displayName = displayName;
        this.unit = unit;
        this.targetMin = targetMin;
        this.targetMax = targetMax;
        this.higherIsBetter = higherIsBetter;
    }

    public boolean isWithinTarget(double value) {
        return value >= targetMin && value <= targetMax;
    }

    public boolean isExcellent(double value) {
        if (higherIsBetter) {
            return value >= targetMax * 0.95;
        } else {
            return value <= targetMin * 1.05;
        }
    }

    public boolean isPoor(double value) {
        if (higherIsBetter) {
            return value < targetMin * 0.8;
        } else {
            return value > targetMax * 1.2;
        }
    }

    public PerformanceLevel getPerformanceLevel(double value) {
        if (isExcellent(value)) return PerformanceLevel.EXCELLENT;
        if (isWithinTarget(value)) return PerformanceLevel.GOOD;
        if (isPoor(value)) return PerformanceLevel.POOR;
        return PerformanceLevel.BELOW_TARGET;
    }

    public String formatValue(double value) {
        return String.format("%.1f %s", value, unit);
    }

    @Override
    public String toString() {
        return displayName + " (" + unit + ")";
    }

    public enum PerformanceLevel {
        EXCELLENT("Отличный", "🟢"),
        GOOD("Хороший", "🟡"),
        BELOW_TARGET("Ниже целевого", "🟠"),
        POOR("Плохой", "🔴");

        private final String name;
        private final String indicator;

        PerformanceLevel(String name, String indicator) {
            this.name = name;
            this.indicator = indicator;
        }

        @Override
        public String toString() {
            return indicator + " " + name;
        }
    }
}