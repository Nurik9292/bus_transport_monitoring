package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public    record PassengerDemandProfile(
        Map<LocalTime, Integer> hourlyDemand,
        Map<DayOfWeek, Integer> weeklyPattern,
        SeasonalVariation seasonalPattern,
        List<DemandPeak> peakPeriods
) {}
