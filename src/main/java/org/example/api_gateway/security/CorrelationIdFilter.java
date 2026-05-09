package org.example.api_gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter extends AbstractGatewayFilterFactory<CorrelationIdFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    public CorrelationIdFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");

            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            final String finalCorrelationId = correlationId;
            log.info("CorrelationId: {} | path: {}", finalCorrelationId, exchange.getRequest().getPath());

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-Correlation-Id", finalCorrelationId)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build())
                    .then(Mono.fromRunnable(() ->
                            exchange.getResponse().getHeaders().add("X-Correlation-Id", finalCorrelationId)
                    ));
        };
    }

    public static class Config {}
}