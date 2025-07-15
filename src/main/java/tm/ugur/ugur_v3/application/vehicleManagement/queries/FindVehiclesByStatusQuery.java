package tm.ugur.ugur_v3.application.vehicleManagement.queries;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import lombok.Builder;
import tm.ugur.ugur_v3.application.shared.commands.Query;
import tm.ugur.ugur_v3.application.shared.pagination.PageRequest;
import tm.ugur.ugur_v3.domain.vehicleManagement.enums.VehicleStatus;

@Builder
public record FindVehiclesByStatusQuery(
        @NotNull(message = "Vehicle status cannot be null")
        VehicleStatus status,

        @NotNull(message = "Page request cannot be null")
        @Valid
        PageRequest pageRequest,

        boolean includeLocationData,
        boolean includeInactiveVehicles,
        boolean requireRecentGps
) implements Query {

    public static FindVehiclesByStatusQuery simple(VehicleStatus status, PageRequest pageRequest) {
        return FindVehiclesByStatusQuery.builder()
                .status(status)
                .pageRequest(pageRequest)
                .includeLocationData(false)
                .includeInactiveVehicles(false)
                .requireRecentGps(false)
                .build();
    }

    public static FindVehiclesByStatusQuery withLocation(VehicleStatus status, PageRequest pageRequest) {
        return FindVehiclesByStatusQuery.builder()
                .status(status)
                .pageRequest(pageRequest)
                .includeLocationData(true)
                .includeInactiveVehicles(false)
                .requireRecentGps(true)
                .build();
    }

    public static FindVehiclesByStatusQuery detailed(VehicleStatus status, PageRequest pageRequest) {
        return FindVehiclesByStatusQuery.builder()
                .status(status)
                .pageRequest(pageRequest)
                .includeLocationData(true)
                .includeInactiveVehicles(true)
                .requireRecentGps(false)
                .build();
    }

    public static FindVehiclesByStatusQuery activeOnly(PageRequest pageRequest) {
        return FindVehiclesByStatusQuery.builder()
                .status(VehicleStatus.ACTIVE)
                .pageRequest(pageRequest)
                .includeLocationData(true)
                .includeInactiveVehicles(false)
                .requireRecentGps(true)
                .build();
    }

    @Override
    public boolean isCacheable() {
        return !includeLocationData && !requireRecentGps;
    }

    @Override
    public java.time.Duration getCacheTtl() {
        if (includeLocationData) {
            return java.time.Duration.ofMinutes(1);
        }
        return java.time.Duration.ofMinutes(5);
    }

    @Override
    public java.time.Duration getTimeout() {
        return includeLocationData ?
                java.time.Duration.ofSeconds(15) :
                java.time.Duration.ofSeconds(10);
    }

    public boolean requiresGpsData() {
        return includeLocationData || requireRecentGps;
    }

    public boolean isRealTimeQuery() {
        return requireRecentGps && includeLocationData;
    }

    public String getQueryDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("FindVehiclesByStatus[").append(status.name()).append("]");

        if (includeLocationData) desc.append("+Location");
        if (requireRecentGps) desc.append("+GPS");
        if (includeInactiveVehicles) desc.append("+Inactive");

        desc.append(" (page=").append(pageRequest.getPageNumber())
                .append(", size=").append(pageRequest.getPageSize()).append(")");

        return desc.toString();
    }
}