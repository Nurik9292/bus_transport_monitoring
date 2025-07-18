package tm.ugur.ugur_v3.domain.routeManagement.services.supporting;

import tm.ugur.ugur_v3.domain.routeManagement.valueobjects.RouteId;
import tm.ugur.ugur_v3.domain.shared.valueobjects.Timestamp;


/**
 * Отзыв пассажира о маршруте
 * TODO: Реализовать в Infrastructure слое при интеграции с системой обратной связи
 */
public record PassengerFeedback(
        String feedbackId,
        RouteId routeId,
        FeedbackType type,
        int rating,
        String comment,
        String category,
        Timestamp submittedAt
) {

    public enum FeedbackType {
        COMPLIMENT,
        COMPLAINT,
        SUGGESTION,
        QUESTION
    }

    public static PassengerFeedback complaint(RouteId routeId, String comment) {
        return new PassengerFeedback(
                java.util.UUID.randomUUID().toString(),
                routeId,
                FeedbackType.COMPLAINT,
                2, // low rating
                comment,
                "SERVICE",
                Timestamp.now()
        );
    }

    public boolean isNegative() {
        return rating <= 2 || type == FeedbackType.COMPLAINT;
    }

    public boolean isPositive() {
        return rating >= 4 || type == FeedbackType.COMPLIMENT;
    }
}