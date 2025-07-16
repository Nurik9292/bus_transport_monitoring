package tm.ugur.ugur_v3.domain.routeManagement.valueobjects;

import lombok.Getter;
import tm.ugur.ugur_v3.domain.shared.exceptions.BusinessRuleViolationException;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
public class DailySchedule {
    private final List<LocalTime> departures;
    private final LocalTime firstDeparture;
    private final LocalTime lastDeparture;
    private final int tripCount;
    private final EstimatedDuration serviceSpan;

    public DailySchedule(List<LocalTime> departures) {
        if (departures == null || departures.isEmpty()) {
            throw new BusinessRuleViolationException("DAILY_SCHEDULE_CANNOT_EMPTY", "Departures cannot be empty");
        }

        this.departures = departures.stream()
                .sorted()
                .collect(Collectors.toList());
        this.firstDeparture = this.departures.getFirst();
        this.lastDeparture = this.departures.getLast();
        this.tripCount = this.departures.size();


        long spanSeconds = lastDeparture.toSecondOfDay() - firstDeparture.toSecondOfDay();
        if (spanSeconds < 0) {
            spanSeconds += 86400;
        }
        this.serviceSpan = EstimatedDuration.ofSeconds(spanSeconds);
    }

    public Optional<LocalTime> getNextDeparture(LocalTime fromTime) {
        return departures.stream()
                .filter(time -> time.isAfter(fromTime))
                .findFirst();
    }

    public List<LocalTime> getAllDepartures() {
        return Collections.unmodifiableList(departures);
    }
}