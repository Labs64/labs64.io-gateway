package io.labs64.apigateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private static final String[] PUBLIC_PATHS = {
        "/public/**",
        "/actuator/**",
        "/v3/api-docs/**"
    };

    private static final String[] PROTECTED_PATHS = {
        "/api/**"
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                // public endpoints
                .pathMatchers(PUBLIC_PATHS).permitAll()
                // protected endpoints
                .pathMatchers(PROTECTED_PATHS).authenticated()
                // ...other requests
                .anyExchange().permitAll()
            )
            // JWT resource server validation
            .oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    public WebFilter requestLoggingFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            logger.trace("Incoming request path: {}", path);

            return ReactiveSecurityContextHolder.getContext()
                    .doOnNext(context -> {
                        Authentication authentication = context.getAuthentication();
                        String authResult = (authentication != null && authentication.isAuthenticated()) ?
                                "Authenticated (Principal: " + authentication.getName() + ")" :
                                "Unauthenticated";
                        logger.trace("Authentication result for path {}: {}", path, authResult);
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        logger.trace("Authentication result for path {}: Unauthenticated (No SecurityContext)", path);
                        return Mono.empty();
                    }))
                    .then(chain.filter(exchange));
        };
    }

}
