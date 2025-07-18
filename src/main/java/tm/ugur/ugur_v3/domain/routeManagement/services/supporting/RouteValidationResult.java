package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import tm.ugur.ugur_v3.domain.routeManagement.services.RouteValidationService;

import java.util.List;

public  record RouteValidationResult(
        boolean isValid,
        RouteValidationService.ValidationSeverity overallSeverity,
        List<RouteValidationService.ValidationIssue> issues,
        List<ValidationWarning> warnings,
        ValidationSummary summary,
        List<String> recommendations
) {}
