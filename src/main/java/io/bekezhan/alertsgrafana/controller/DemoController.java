package io.bekezhan.alertsgrafana.controller;

import io.bekezhan.alertsgrafana.service.OrderService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * Demo REST API — 3 business endpoints + health-check + flaky endpoint.
 *
 * GET  /api/health           → simple health probe (не actuator)
 * GET  /api/orders           → list orders (stub, fast)
 * POST /api/orders           → create order (business metric)
 * GET  /api/slow             → artificial delay (latency alert trigger)
 * GET  /api/flaky            → random 500 (error-rate alert trigger)
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DemoController {

    private final OrderService orderService;

    // Health-check (application-level, distinct from /actuator/health)
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        orderService.updateActiveUsers();
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "observability-demo",
                "timestamp", Instant.now().toString()
        ));
    }

    // Normal business endpoints
    @Timed(value = "api.orders.list", description = "Time to list all orders")
    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> listOrders() {
        log.info("GET /api/orders");
        return ResponseEntity.ok(Map.of(
                "orders", java.util.List.of(
                        Map.of("id", "A1B2C3", "status", "DELIVERED"),
                        Map.of("id", "D4E5F6", "status", "PENDING"),
                        Map.of("id", "G7H8I9", "status", "SHIPPED")
                ),
                "total", 3
        ));
    }

    @Timed(value = "api.orders.create", description = "Time to create an order")
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> createOrder() {
        log.info("POST /api/orders");
        Map<String, Object> result = orderService.createOrder();
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // Degradation triggers (for alert testing)
    /**
     * Slow endpoint — sleeps for `app.slow-endpoint.delay-ms` milliseconds.
     * Hit this repeatedly to trigger the High Latency alert.
     */
    @Timed(value = "api.slow", description = "Intentionally slow endpoint")
    @GetMapping("/slow")
    public ResponseEntity<Map<String, Object>> slow() throws InterruptedException {
        log.warn("GET /api/slow — artificial delay in progress");
        Map<String, Object> result = orderService.processSlowOperation();
        return ResponseEntity.ok(result);
    }

    /**
     * Flaky endpoint — returns 500 ~50% of the time (configurable via app.error-endpoint.error-rate).
     * Hit this repeatedly to trigger the High Error Rate alert.
     */
    @Timed(value = "api.flaky", description = "Randomly failing endpoint for alert testing")
    @GetMapping("/flaky")
    public ResponseEntity<Map<String, Object>> flaky() {
        log.info("GET /api/flaky");

        if (orderService.shouldFail()) {
            log.error("GET /api/flaky → 500 (intentional)");
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Intentional server error",
                            "hint", "This is expected — used to test High Error Rate alert"
                    ));
        }

        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "message", "Lucky! No error this time."
        ));
    }

    // Exception handler — ensures 5xx still get recorded by Micrometer
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", ex.getMessage()));
    }
}