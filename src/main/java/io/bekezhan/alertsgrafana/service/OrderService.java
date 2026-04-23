package io.bekezhan.alertsgrafana.service;

import io.bekezhan.alertsgrafana.metrics.BusinessMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final BusinessMetrics metrics;
    private final Random random = new Random();

    @Value("${app.slow-endpoint.delay-ms:2000}")
    private long slowDelayMs;

    @Value("${app.error-endpoint.error-rate:0.5}")
    private double errorRate;

    /**
     * Simulates creating an order (fast, happy-path).
     */
    public Map<String, Object> createOrder() {
        return metrics.getOrderProcessingTimer().record(() -> {
            String orderId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            metrics.incrementOrders();
            log.info("Order created: {}", orderId);
            return Map.of(
                    "orderId", orderId,
                    "status", "CREATED",
                    "message", "Order successfully placed"
            );
        });
    }

    /**
     * Simulates a slow operation (configurable delay).
     * Use to trigger High Latency alert.
     */
    public Map<String, Object> processSlowOperation() throws InterruptedException {
        long start = System.currentTimeMillis();
        metrics.incrementSlowRequests();

        log.warn("Starting slow operation (delay={}ms)", slowDelayMs);
        Thread.sleep(slowDelayMs);

        long elapsed = System.currentTimeMillis() - start;
        metrics.recordSlowResponseMs(elapsed);

        log.warn("Slow operation finished in {}ms", elapsed);
        return Map.of(
                "status", "OK",
                "message", "Slow operation completed",
                "durationMs", elapsed
        );
    }

    /**
     * Randomly returns 500 based on configured error rate.
     * Use to trigger High Error Rate alert.
     */
    public boolean shouldFail() {
        boolean fail = random.nextDouble() < errorRate;
        if (fail) {
            metrics.incrementFlakyErrors();
        }
        return fail;
    }

    /**
     * Simulates active user count fluctuation.
     */
    public void updateActiveUsers() {
        metrics.setActiveUsers(random.nextInt(50) + 1);
    }
}