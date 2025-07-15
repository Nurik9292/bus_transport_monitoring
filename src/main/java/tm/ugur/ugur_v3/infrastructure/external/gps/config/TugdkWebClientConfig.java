package tm.ugur.ugur_v3.infrastructure.external.gps.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(TugdkGpsProperties.class)
public class TugdkWebClientConfig {

    private final TugdkGpsProperties properties;

    @Bean("tugdkWebClient")
    public WebClient tugdkWebClient() {
        return WebClient.builder()
                .baseUrl(properties.getUrl())
                .clientConnector(createOptimizedConnector())
                .defaultHeader("Authorization", "Bearer " + properties.getBearerToken())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "Transport-Monitoring-System/1.0")
                .filter(logRequest())
                .filter(logResponse())
                .filter(handleErrors())
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(1024 * 1024)
                )
                .build();
    }

    private ReactorClientHttpConnector createOptimizedConnector() {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("tugdk-gps")
                .maxConnections(50)
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofMinutes(5))
                .pendingAcquireTimeout(Duration.ofSeconds(5))
                .evictInBackground(Duration.ofSeconds(30))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) properties.getTimeout().toMillis())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(
                                properties.getTimeout().toSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(
                                properties.getTimeout().toSeconds(), TimeUnit.SECONDS))
                )
                .compress(true)
                .keepAlive(true);

        return new ReactorClientHttpConnector(httpClient);
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            if (log.isDebugEnabled()) {
                log.debug("TUGDK GPS API Request: {} {}",
                        request.method(), request.url());
            }
            return Mono.just(request);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (log.isDebugEnabled()) {
                log.debug("TUGDK GPS API Response: {} {} ({}ms)",
                        response.request().getMethod(),
                        response.request().getURI(),
                        response.headers().header("X-Response-Time"));
            }
            return Mono.just(response);
        });
    }

    private ExchangeFilterFunction handleErrors() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode().isError()) {
                log.warn("TUGDK GPS API Error: {} {} - Status: {}",
                        response.request().getMethod(),
                        response.request().getURI(),
                        response.statusCode());


                return response.bodyToMono(String.class)
                        .doOnNext(body -> log.error("TUGDK GPS API Error Body: {}", body))
                        .then(Mono.just(response));
            }
            return Mono.just(response);
        });
    }
}