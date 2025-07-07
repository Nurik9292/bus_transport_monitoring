package tm.ugur.ugur_v3.application.shared.executor;

import reactor.core.publisher.Mono;

public interface UseCaseExecutor {
    <INPUT, OUTPUT> Mono<OUTPUT> execute(UseCase<INPUT, OUTPUT> useCase, INPUT input);
    <INPUT, OUTPUT> Mono<OUTPUT> execute(UseCase<INPUT, OUTPUT> useCase, Mono<INPUT> input);
}
