package tm.ugur.ugur_v3.application.vehicleManagement.queries;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tm.ugur.ugur_v3.application.shared.commands.Query;

import java.time.Duration;

public record FindVehicleByLicensePlateQuery(
        @NotEmpty(message = "License plate cannot be empty")
        @Size(min = 2, max = 20, message = "License plate must be between 2 and 20 characters")
        @Pattern(regexp = "^[A-Z0-9\\-\\s]+$", message = "License plate contains invalid characters")
        String licensePlate,

        boolean exactMatch,
        boolean useCache
) implements Query {

    public static FindVehicleByLicensePlateQuery exact(String licensePlate) {
        return new FindVehicleByLicensePlateQuery(
                licensePlate.trim().toUpperCase(),
                true,
                true
        );
    }

    public static FindVehicleByLicensePlateQuery fuzzy(String licensePlate) {
        return new FindVehicleByLicensePlateQuery(
                licensePlate.trim().toUpperCase(),
                false,
                true
        );
    }

    public static FindVehicleByLicensePlateQuery realTime(String licensePlate) {
        return new FindVehicleByLicensePlateQuery(
                licensePlate.trim().toUpperCase(),
                true,
                false
        );
    }

    @Override
    public boolean isCacheable() {
        return useCache && exactMatch;
    }

    @Override
    public Duration getCacheTtl() {
        return Duration.ofMinutes(10);
    }

    @Override
    public Duration getTimeout() {
        return exactMatch ?
                Duration.ofSeconds(5) :
                Duration.ofSeconds(10);
    }

    public String getNormalizedLicensePlate() {
        return licensePlate.replaceAll("[\\s\\-]", "").toUpperCase();
    }

    public boolean isPartialSearch() {
        return !exactMatch || licensePlate.length() < 6;
    }
}