package tm.ugur.ugur_v3.domain.shared.repositories;

import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.domain.shared.entities.AggregateRoot;
import tm.ugur.ugur_v3.domain.shared.valueobjects.EntityId;

import java.util.concurrent.CompletableFuture;

public interface Repository<T extends AggregateRoot<ID>, ID extends EntityId> {

    Mono<T> save(T aggregate);

    Mono<T> findById(ID id);

    Mono<Boolean> existsById(ID id);

    Mono<T> delete(T aggregate);

    Mono<Void> deleteById(ID id);

    Mono<T> count();

    CompletableFuture<ID> nextId();
}