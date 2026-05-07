package org.example.api_gateway.config;

import org.example.api_gateway.security.CorrelationIdFilter;
import org.example.api_gateway.security.JwtAuthFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CorrelationIdFilter correlationIdFilter;

    public GatewayConfig(JwtAuthFilter jwtAuthFilter, CorrelationIdFilter correlationIdFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.correlationIdFilter = correlationIdFilter;
    }

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r
                        .path("/auth/**")
                        .filters(f -> f.filter(correlationIdFilter.apply(new CorrelationIdFilter.Config())))
                        .uri("http://localhost:8081"))

                .route("inventory-service", r -> r
                        .path("/products/**", "/customers/**")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new CorrelationIdFilter.Config()))
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("http://localhost:8082"))

                .route("order-service", r -> r
                        .path("/orders/**", "/invoices/**")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new CorrelationIdFilter.Config()))
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("http://localhost:8083"))

                .route("report-service", r -> r
                        .path("/reports/**")
                        .filters(f -> f
                                .filter(correlationIdFilter.apply(new CorrelationIdFilter.Config()))
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("http://localhost:8084"))

                .build();
    }
}