package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import java.util.List;

public record ScheduleValidationResult(
        boolean isValid,
        List<ScheduleValidationIssue> issues,
        ScheduleQualityAssessment qualityAssessment,
        OperationalFeasibility feasibility
) {}
