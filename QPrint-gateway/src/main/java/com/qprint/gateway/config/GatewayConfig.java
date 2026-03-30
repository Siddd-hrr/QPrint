package com.qprint.gateway.config;

import com.qprint.gateway.filter.ResponseJsonContentTypeFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final ResponseJsonContentTypeFilter jsonContentTypeFilter;

    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {
        // Routes also defined in application.yml; this ensures JwtAuth filter bean is available.
        return builder.routes().build();
    }

    @Bean
    public GlobalFilter responseContentTypeFilter() {
        return jsonContentTypeFilter;
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            var address = exchange.getRequest().getRemoteAddress();
            String ip = address != null && address.getAddress() != null ? address.getAddress().getHostAddress() : "unknown";
            return Mono.just(ip);
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(org.springframework.core.env.Environment env) {
        String allowedOrigins = env.getProperty("cors.allowed-origins", "http://localhost:5173,http://localhost:5174,http://localhost:5175");
        CorsConfiguration config = new CorsConfiguration();
        for (String origin : allowedOrigins.split(",")) {
            config.addAllowedOrigin(origin.trim());
        }
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
