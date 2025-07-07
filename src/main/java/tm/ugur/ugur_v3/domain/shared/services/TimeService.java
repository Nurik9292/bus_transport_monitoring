package tm.ugur.ugur_v3.domain.shared.services;

import java.time.LocalDateTime;

public interface TimeService {
    LocalDateTime now();
    LocalDateTime nowUTC();
    long currentTimeMillis();

    default boolean isRecent(LocalDateTime timestamp, long maxAgeMinutes) {
        return timestamp.isAfter(now().minusMinutes(maxAgeMinutes));
    }
}
