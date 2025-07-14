package tm.ugur.ugur_v3.infrastructure.external.gps.dto;

public record GpsDataDto(
        String vehicleId,
        double latitude,
        double longitude,
        double accuracy,
        Double speed,
        Double bearing,
        java.time.Instant timestamp,
        String source,
        java.util.Map<String, Object> metadata
) {

    public static GpsDataDto fromTugdk(TugdkGpsDataDto tugdkDto) {
        return new GpsDataDto(
                tugdkDto.attributes().name(),
                tugdkDto.latitude(),
                tugdkDto.longitude(),
                tugdkDto.accuracy(),
                tugdkDto.speed(),
                tugdkDto.course(),
                Instant.parse(tugdkDto.fixTime()),
                "TUGDK",
                Map.of("deviceId", tugdkDto.deviceId(), "protocol", tugdkDto.protocol())
        );
    }

    public static GpsDataDto fromAyauk(AyaukGpsDataDto ayaukDto) {
        return new GpsDataDto(
                ayaukDto.carNumber(),
                0.0, 0.0, 0.0, // AYAUK не предоставляет GPS координаты
                null, null,
                Instant.now(),
                "AYAUK",
                Map.of("routeNumber", ayaukDto.number(), "change", ayaukDto.change())
        );
    }
}
