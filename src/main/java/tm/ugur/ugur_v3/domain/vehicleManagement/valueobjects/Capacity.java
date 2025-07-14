package tm.ugur.ugur_v3.domain.vehicleManagement.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.valueobjects.ValueObject;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleType;

@Getter
public final class Capacity extends ValueObject {


    private static final int MIN_SEATED_CAPACITY = 1;
    private static final int MAX_SEATED_CAPACITY = 200;
    private static final int MAX_STANDING_CAPACITY = 300;
    private static final int MAX_TOTAL_CAPACITY = 400;


    private static final double MAX_STANDING_TO_SEATED_RATIO = 2.0;
    private static final double MIN_SEATED_PERCENTAGE = 0.25;

    private final int seatedCapacity;
    private final int standingCapacity;
    private final int totalCapacity;

    private Capacity(int seatedCapacity, int standingCapacity) {
        this.seatedCapacity = seatedCapacity;
        this.standingCapacity = standingCapacity;
        this.totalCapacity = seatedCapacity + standingCapacity;
        validate();
    }

    public static Capacity of(int seatedCapacity, int standingCapacity) {
        return new Capacity(seatedCapacity, standingCapacity);
    }

    public static Capacity seatedOnly(int seatedCapacity) {
        return of(seatedCapacity, 0);
    }

    public static Capacity fromTotal(int totalCapacity) {
        if (totalCapacity <= 0) {
            throw new BusinessRuleViolationException(
                    "Total capacity must be positive",
                    "INVALID_TOTAL_CAPACITY"
            );
        }

        int seated = (int) Math.ceil(totalCapacity * 0.6);
        int standing = totalCapacity - seated;

        return of(seated, standing);
    }

    public static Capacity standardFor(VehicleType vehicleType) {
        return switch (vehicleType) {
            case BUS -> of(45, 60);
            case TROLLEY -> of(40, 55);
            case TRAM -> of(50, 80);
            case MINIBUS -> of(15, 10);

        };
    }

    @Override
    protected void validate() {
        validateSeatedCapacity();
        validateStandingCapacity();
        validateTotalCapacity();
        validateSafetyRatios();
    }

    private void validateSeatedCapacity() {
        if (seatedCapacity < MIN_SEATED_CAPACITY) {
            throw new BusinessRuleViolationException(
                    String.format("Seated capacity must be at least %d, got: %d", MIN_SEATED_CAPACITY, seatedCapacity),
                    "INVALID_SEATED_CAPACITY_TOO_LOW"
            );
        }

        if (seatedCapacity > MAX_SEATED_CAPACITY) {
            throw new BusinessRuleViolationException(
                    String.format("Seated capacity exceeds safety limit of %d, got: %d", MAX_SEATED_CAPACITY, seatedCapacity),
                    "INVALID_SEATED_CAPACITY_TOO_HIGH"
            );
        }
    }

    private void validateStandingCapacity() {
        if (standingCapacity < 0) {
            throw new BusinessRuleViolationException(
                    "Standing capacity cannot be negative",
                    "INVALID_STANDING_CAPACITY_NEGATIVE"
            );
        }

        if (standingCapacity > MAX_STANDING_CAPACITY) {
            throw new BusinessRuleViolationException(
                    String.format("Standing capacity exceeds safety limit of %d, got: %d", MAX_STANDING_CAPACITY, standingCapacity),
                    "INVALID_STANDING_CAPACITY_TOO_HIGH"
            );
        }
    }

    private void validateTotalCapacity() {
        if (totalCapacity > MAX_TOTAL_CAPACITY) {
            throw new BusinessRuleViolationException(
                    String.format("Total capacity exceeds safety limit of %d, got: %d (seated: %d, standing: %d)",
                            MAX_TOTAL_CAPACITY, totalCapacity, seatedCapacity, standingCapacity),
                    "INVALID_TOTAL_CAPACITY_TOO_HIGH"
            );
        }
    }

    private void validateSafetyRatios() {
        if (standingCapacity > 0) {
            double standingToSeatedRatio = (double) standingCapacity / seatedCapacity;
            if (standingToSeatedRatio > MAX_STANDING_TO_SEATED_RATIO) {
                throw new BusinessRuleViolationException(
                        String.format("Standing-to-seated ratio %.2f exceeds safety limit of %.2f",
                                standingToSeatedRatio, MAX_STANDING_TO_SEATED_RATIO),
                        "INVALID_CAPACITY_RATIO"
                );
            }

            double seatedPercentage = (double) seatedCapacity / totalCapacity;
            if (seatedPercentage < MIN_SEATED_PERCENTAGE) {
                throw new BusinessRuleViolationException(
                        String.format("Seated capacity must be at least %.0f%% of total, got: %.1f%%",
                                MIN_SEATED_PERCENTAGE * 100, seatedPercentage * 100),
                        "INVALID_SEATED_PERCENTAGE"
                );
            }
        }
    }

    public double calculateUtilization(int currentPassengers) {
        if (currentPassengers < 0) {
            return 0.0;
        }

        return Math.min(100.0, (double) currentPassengers / totalCapacity * 100);
    }

    public boolean isCrowded(int currentPassengers) {
        return calculateUtilization(currentPassengers) > 85.0;
    }

    public boolean isAtCapacity(int currentPassengers) {
        return currentPassengers >= totalCapacity;
    }

    public boolean canAccommodate(int currentPassengers, int additionalPassengers) {
        return (currentPassengers + additionalPassengers) <= totalCapacity;
    }

    public int getAvailableCapacity(int currentPassengers) {
        return Math.max(0, totalCapacity - currentPassengers);
    }

    public ComfortLevel getComfortLevel(int currentPassengers) {
        double utilization = calculateUtilization(currentPassengers);

        if (utilization <= 60.0) {
            return ComfortLevel.COMFORTABLE;
        } else if (utilization <= 85.0) {
            return ComfortLevel.MODERATE;
        } else if (utilization < 100.0) {
            return ComfortLevel.CROWDED;
        } else {
            return ComfortLevel.FULL;
        }
    }

    public boolean isValidForVehicleType(VehicleType vehicleType) {
        Capacity standard = standardFor(vehicleType);


        double tolerance = 0.20;
        int minTotal = (int) (standard.getTotalCapacity() * (1 - tolerance));
        int maxTotal = (int) (standard.getTotalCapacity() * (1 + tolerance));

        return totalCapacity >= minTotal && totalCapacity <= maxTotal;
    }

    public boolean isSuitableForPublicTransport() {
        return totalCapacity >= 30 &&
                seatedCapacity >= 15 &&
                (standingCapacity == 0 || (double) standingCapacity / seatedCapacity <= MAX_STANDING_TO_SEATED_RATIO);
    }


    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{seatedCapacity, standingCapacity};
    }

    @Override
    public String toString() {
        if (standingCapacity == 0) {
            return String.format("Capacity{seated=%d}", seatedCapacity);
        }
        return String.format("Capacity{seated=%d, standing=%d, total=%d}",
                seatedCapacity, standingCapacity, totalCapacity);
    }

    public String getDisplayFormat() {
        if (standingCapacity == 0) {
            return String.format("%d seats", seatedCapacity);
        }
        return String.format("%d seats + %d standing = %d total",
                seatedCapacity, standingCapacity, totalCapacity);
    }

    @Getter
    public enum ComfortLevel {
        COMFORTABLE("Comfortable", "green"),
        MODERATE("Moderate", "yellow"),
        CROWDED("Crowded", "orange"),
        FULL("Full", "red");

        private final String description;
        private final String colorCode;

        ComfortLevel(String description, String colorCode) {
            this.description = description;
            this.colorCode = colorCode;
        }

    }
}