package tm.ugur.ugur_v3.domain.shared.events;

public interface DomainEventListener {

    boolean canHandle(DomainEvent event);

    void handle(DomainEvent event);

    default int getPriority() {
        return Integer.MAX_VALUE;
    }
}
