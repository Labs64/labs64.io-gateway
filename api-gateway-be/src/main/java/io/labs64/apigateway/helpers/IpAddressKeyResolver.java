package io.labs64.apigateway.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

@Component
public class IpAddressKeyResolver implements KeyResolver {

    private static final Logger logger = LoggerFactory.getLogger(IpAddressKeyResolver.class);

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        Mono<String> ipAddress = Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(InetSocketAddress::getAddress)
                .map(InetAddress::getHostAddress)
                .map(Mono::just)
                .orElse(Mono.empty());
        return ipAddress.doOnNext(ip -> logger.debug("Resolved IP address: {}", ip))
                .doOnError(e -> logger.error("Error resolving IP address: {}", e.getMessage()));
    }

}
