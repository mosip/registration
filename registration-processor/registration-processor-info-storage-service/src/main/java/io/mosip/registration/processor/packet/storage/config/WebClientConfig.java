package io.mosip.registration.processor.packet.storage.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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

@Configuration
public class WebClientConfig {

    @Value("${webclient.connection.timeout.ms:6000}")
    private int connectionTimeoutMs;

    @Value("${webclient.read.timeout.ms:30000}")
    private int readTimeoutMs;

    @Value("${webclient.write.timeout.ms:10000}")
    private int writeTimeoutMs;

    @Value("${webclient.max.connections:200}")
    private int maxConnections;

    @Value("${webclient.pending.acquire.max.count:400}")
    private int pendingAcquireMaxCount;

    @Value("${webclient.pending.acquire.timeout.ms:45000}")
    private long pendingAcquireTimeoutMs;

    @Value("${webclient.max.idle.time.ms:20000}")
    private long maxIdleTimeMs;

    @Value("${webclient.max.life.time.ms:60000}")
    private long maxLifeTimeMs;

    @Value("${webclient.evict.interval.ms:30000}")
    private long evictIntervalMs;

    @Value("${webclient.response.buffer.size.mb:16}")
    private int responseBufferSizeMb;

    /**
     * Instead of replacing the library bean:
     * 1. Inject the library's WebClient as-is (it has the token)
     * 2. Extract its filters (token logic lives here)
     * 3. Rebuild with same filters + your performance Netty config
     * 4. Register as a DIFFERENT bean name
     *
     * PacketManagerService then injects THIS bean via @Qualifier
     */
    @Bean("packetManagerWebClient")
    public WebClient packetManagerWebClient(
            @Qualifier("selfTokenWebClient") WebClient libraryWebClient) {

        // --- 1. Extract token filters from library's WebClient ---
        // WebClient internally holds filters in its builder state.
        // We mutate from the existing instance — this preserves ALL
        // filters the library registered (token refresh, auth headers etc.)
        WebClient.Builder builder = libraryWebClient.mutate();

        // --- 2. Tuned Connection Pool ---
        ConnectionProvider connectionProvider = ConnectionProvider
                .builder("packet-manager-pool")
                .maxConnections(maxConnections)
                .pendingAcquireMaxCount(pendingAcquireMaxCount)
                .pendingAcquireTimeout(Duration.ofMillis(pendingAcquireTimeoutMs))
                .maxIdleTime(Duration.ofMillis(maxIdleTimeMs))
                .maxLifeTime(Duration.ofMillis(maxLifeTimeMs))
                .evictInBackground(Duration.ofMillis(evictIntervalMs))
                .metrics(true)
                .build();

        // --- 3. Tuned Netty HttpClient ---
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMs)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .resolver(DefaultAddressResolverGroup.INSTANCE)
                .responseTimeout(Duration.ofMillis(readTimeoutMs))
                .doOnConnected(conn -> conn
                        .addHandlerLast(
                                new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS))
                        .addHandlerLast(
                                new WriteTimeoutHandler(writeTimeoutMs, TimeUnit.MILLISECONDS))
                )
                .compress(true);

        // --- 4. Override only the connector and codecs ---
        // Token filters from library are already in builder via mutate()
        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(responseBufferSizeMb * 1024 * 1024))
                .filter(loggingFilter())
                .build();
    }

    private ExchangeFilterFunction loggingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            System.out.println("[PacketManager WebClient] --> "
                    + request.method() + " " + request.url());
            return Mono.just(request);
        });
    }
}