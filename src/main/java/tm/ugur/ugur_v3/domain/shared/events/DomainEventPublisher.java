package tm.ugur.ugur_v3.domain.shared.events;

import java.util.List;

public interface DomainEventPublisher extends DomainEvent {
    void publish(DomainEvent event);
    void publishAll(List<DomainEvent> events);
}
