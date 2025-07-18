package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import tm.ugur.ugur_v3.domain.routeManagement.services.RouteValidationService;

import java.util.List;

public record ValidationIssue(
        String issueType,
        RouteValidationService.ValidationSeverity severity,
        String description,
        String location,
        List<String> suggestedFixes
) {}