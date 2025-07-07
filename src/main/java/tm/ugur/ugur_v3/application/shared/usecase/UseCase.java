package tm.ugur.ugur_v3.application.shared.usecase;

import reactor.core.publisher.Mono;

public interface UseCase<INPUT, OUTPUT> {
    Mono<OUTPUT> execute(INPUT input);
}
