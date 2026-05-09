package org.example.api_gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AuthVerifyFilter extends AbstractGatewayFilterFactory<AuthVerifyFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthVerifyFilter.class);
    private final WebClient webClient;

    public AuthVerifyFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081").build();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            return webClient.get()
                    .uri("/auth/verify")
                    .header("Authorization", authHeader)
                    .retrieve()
                    .bodyToMono(VerifyResponse.class)
                    .flatMap(verifyResponse -> {
                        log.info("Verified user: {} | role: {}",
                                verifyResponse.getUsername(), verifyResponse.getRole());

                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                .header("X-Auth-Username", verifyResponse.getUsername())
                                .header("X-Auth-Role", verifyResponse.getRole())
                                .build();

                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    })
                    .onErrorResume(e -> {
                        log.warn("Token verification failed: {}", e.getMessage());
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    });
        };
    }

    public static class VerifyResponse {
        private String username;
        private String role;

        public String getUsername() { return username; }
        public String getRole() { return role; }
        public void setUsername(String username) { this.username = username; }
        public void setRole(String role) { this.role = role; }
    }

    public static class Config {}
}