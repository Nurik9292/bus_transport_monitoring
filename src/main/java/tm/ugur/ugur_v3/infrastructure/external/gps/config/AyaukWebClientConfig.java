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
@EnableConfigurationProperties(AyaukGpsProperties.class)
public class AyaukWebClientConfig {

    private final AyaukGpsProperties properties;

    @Bean("ayaukWebClient")
    public WebClient ayaukWebClient() {
        String basicAuth = "Basic " + properties.getBasicAuthCredentials();

        return WebClient.builder()
                .baseUrl(properties.getUrl())
                .clientConnector(createOptimizedConnector())
                .defaultHeader("Authorization", basicAuth)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "Transport-Monitoring-System/1.0")
                .filter(logRequest())
                .filter(logResponse())
                .filter(handleErrors())
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(512 * 1024) // 512KB buffer for route assignment data
                )
                .build();
    }

    private ReactorClientHttpConnector createOptimizedConnector() {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("ayauk-routes")
                .maxConnections(20) // Fewer connections than TUGDK
                .maxIdleTime(Duration.ofSeconds(60))
                .maxLifeTime(Duration.ofMinutes(10))
                .pendingAcquireTimeout(Duration.ofSeconds(10))
                .evictInBackground(Duration.ofSeconds(60))
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
                log.debug("AYAUK API Request: {} {}",
                        request.method(), request.url());
            }
            return Mono.just(request);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (log.isDebugEnabled()) {
                log.debug("AYAUK API Response: {} {} ({}ms)",
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
                log.warn("AYAUK API Error: {} {} - Status: {}",
                        response.request().getMethod(),
                        response.request().getURI(),
                        response.statusCode());

                if (response.statusCode().value() == 401) {
                    log.error("AYAUK API Authentication failed - check username/password");
                } else if (response.statusCode().value() == 403) {
                    log.error("AYAUK API Access forbidden - check permissions");
                }

                return response.bodyToMono(String.class)
                        .doOnNext(body -> log.error("AYAUK API Error Body: {}", body))
                        .then(Mono.just(response));
            }
            return Mono.just(response);
        });
    }
}