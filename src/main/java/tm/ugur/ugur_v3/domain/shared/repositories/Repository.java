package tm.ugur.ugur_v3.domain.shared.repositories;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tm.ugur.ugur_v3.domain.shared.entities.AggregateRoot;
import tm.ugur.ugur_v3.domain.shared.valueobjects.EntityId;

public interface Repository<T extends AggregateRoot<ID>, ID extends EntityId> {

    Mono<T> save(T aggregate);

    Mono<T> findById(ID id);

    Mono<Boolean> existsById(ID id);

    Mono<T> delete(T aggregate);

    Mono<Void> deleteById(ID id);

    Mono<Long> count();

    Mono<ID> nextId();

    Flux<T> findAll();

    default Flux<T> findAll(int offset, int limit) {
        return findAll().skip(offset).take(limit);
    }

    default Mono<Boolean> isHealthy() {
        return count().map(c -> c >= 0).onErrorReturn(false);
    }

    default Mono<Void> clearCache() {
        return Mono.empty();
    }
}