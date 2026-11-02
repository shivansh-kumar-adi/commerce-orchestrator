package com.orderflow.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SecurityAndCorrelationFilterTest {
    private static final String SECRET = "test-secret-must-be-at-least-32-characters-long";
    private final SecurityAndCorrelationFilter filter = new SecurityAndCorrelationFilter(SECRET);

    @Test void rejectsMissingToken() {
        var exchange = exchange(HttpMethod.GET, "/api/orders", null);
        filter.filter(exchange, ignored -> Mono.empty()).block();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test void forbidsCustomerFromInventoryAdministration() {
        var exchange = exchange(HttpMethod.PUT, "/api/inventory/00000000-0000-0000-0000-000000000001", token("CUSTOMER"));
        filter.filter(exchange, ignored -> Mono.empty()).block();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test void replacesSpoofedIdentityHeaders() {
        var request = MockServerHttpRequest.get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("CUSTOMER"))
                .header(SecurityAndCorrelationFilter.USER_ID, "attacker")
                .header(SecurityAndCorrelationFilter.USER_ROLE, "ADMIN").build();
        var exchange = MockServerWebExchange.from(request);
        var forwarded = new AtomicReference<org.springframework.http.server.reactive.ServerHttpRequest>();
        filter.filter(exchange, next -> { forwarded.set(next.getRequest()); return Mono.empty(); }).block();
        assertEquals("user-123", forwarded.get().getHeaders().getFirst(SecurityAndCorrelationFilter.USER_ID));
        assertEquals("CUSTOMER", forwarded.get().getHeaders().getFirst(SecurityAndCorrelationFilter.USER_ROLE));
        assertNotNull(exchange.getResponse().getHeaders().getFirst(SecurityAndCorrelationFilter.CORRELATION_ID));
    }

    private MockServerWebExchange exchange(HttpMethod method, String path, String token) {
        var builder = MockServerHttpRequest.method(method, path);
        if (token != null) builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return MockServerWebExchange.from(builder.build());
    }

    private String token(String role) {
        var key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder().subject("user-123").claim("role", role)
                .expiration(Date.from(Instant.now().plusSeconds(300))).signWith(key).compact();
    }
}
