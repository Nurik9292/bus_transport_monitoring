package tm.ugur.ugur_v3.domain.shared.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DomainEventPublisher {

    private final List<DomainEventListener> listeners = new CopyOnWriteArrayList<>();
    private final ThreadLocal<List<DomainEvent>> events = ThreadLocal.withInitial(CopyOnWriteArrayList::new);

    private static final DomainEventPublisher INSTANCE = new DomainEventPublisher();

    private DomainEventPublisher() {}

    public static DomainEventPublisher instance() {
        return INSTANCE;
    }

    public void subscribe(DomainEventListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void unsubscribe(DomainEventListener listener) {
        listeners.remove(listener);
    }

    public void raise(DomainEvent event) {
        if (event != null) {
            events.get().add(event);
        }
    }

    public void publishAll() {
        List<DomainEvent> currentEvents = events.get();
        if (currentEvents.isEmpty()) {
            return;
        }

        currentEvents.sort((e1, e2) ->
                Long.compare(e1.getOccurredAt().getEpochMillis(), e2.getOccurredAt().getEpochMillis()));

        for (DomainEvent event : currentEvents) {
            for (DomainEventListener listener : listeners) {
                try {
                    if (listener.canHandle(event)) {
                        listener.handle(event);
                    }
                } catch (Exception e) {
                    System.err.println("Error handling event " + event.getEventType() + ": " + e.getMessage());
                }
            }
        }

        clear();
    }

    public void clear() {
        events.get().clear();
    }

    public List<DomainEvent> getUnpublishedEvents() {
        return new CopyOnWriteArrayList<>(events.get());
    }

    public boolean hasUnpublishedEvents() {
        return !events.get().isEmpty();
    }
}

