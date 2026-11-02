package com.orderflow.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) { SpringApplication.run(GatewayApplication.class, args); }
}

@Component
class SecurityAndCorrelationFilter implements GlobalFilter, Ordered {
    static final String USER_ID = "X-User-ID";
    static final String USER_ROLE = "X-User-Role";
    static final String CORRELATION_ID = "X-Correlation-ID";
    private final byte[] secret;

    SecurityAndCorrelationFilter(@Value("${jwt.secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public int getOrder() { return -100; }

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) correlationId = UUID.randomUUID().toString();
        exchange.getResponse().getHeaders().set(CORRELATION_ID, correlationId);

        var request = exchange.getRequest().mutate().headers(headers -> {
            headers.remove(USER_ID);
            headers.remove(USER_ROLE);
            headers.remove(CORRELATION_ID);
        }).header(CORRELATION_ID, correlationId);

        String path = exchange.getRequest().getURI().getPath();
        boolean publicPath = path.startsWith("/api/auth/")
                || (exchange.getRequest().getMethod() == HttpMethod.GET && path.startsWith("/api/products"))
                || path.equals("/actuator/health");
        if (publicPath) return chain.filter(exchange.mutate().request(request.build()).build());

        String role;
        try {
            String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authorization == null || !authorization.startsWith("Bearer ")) throw new IllegalArgumentException();
            var claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret)).build()
                    .parseSignedClaims(authorization.substring(7)).getPayload();
            role = claims.get("role", String.class);
            request.header(USER_ID, claims.getSubject()).header(USER_ROLE, role);
        } catch (Exception ignored) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        boolean administrative = path.startsWith("/api/inventory/")
                || (path.startsWith("/api/products") && exchange.getRequest().getMethod() != HttpMethod.GET);
        if (administrative && !"ADMIN".equals(role)) return reject(exchange, HttpStatus.FORBIDDEN);
        return chain.filter(exchange.mutate().request(request.build()).build());
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}
