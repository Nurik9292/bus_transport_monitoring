package tm.ugur.ugur_v3.domain.routeManagement.valueobjects;

import tm.ugur.ugur_v3.domain.routeManagement.enums.ServiceFrequency;

import java.util.HashMap;
import java.util.Map;


public record FrequencyPattern(Map<String, Integer> periodHeadways, ServiceFrequency defaultFrequency) {

   public  FrequencyPattern(Map<String, Integer> periodHeadways, ServiceFrequency defaultFrequency) {
        this.periodHeadways = new HashMap<>(periodHeadways);
        this.defaultFrequency = defaultFrequency;
    }

    public int getHeadwayForPeriod(String periodName) {
        return periodHeadways.getOrDefault(periodName, defaultFrequency.getMinHeadwayMinutes());
    }
}