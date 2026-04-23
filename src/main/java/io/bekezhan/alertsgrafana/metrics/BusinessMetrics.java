package io.bekezhan.alertsgrafana.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom business metrics — bonus requirement.
 * Tracks: order counts, active users, flaky errors.
 */
@Component
public class BusinessMetrics {

    // Counters
    private final Counter ordersCreated;
    private final Counter flakyErrors;
    private final Counter slowRequests;

    // Gauges (backed by atomics so Gauge can read live values)
    private final AtomicInteger activeUsers = new AtomicInteger(0);
    private final AtomicLong lastSlowResponseMs = new AtomicLong(0);

    // Timer for business-level order processing
    @Getter
    private final Timer orderProcessingTimer;

    public BusinessMetrics(MeterRegistry registry) {
        this.ordersCreated = Counter.builder("business.orders.created.total")
                .description("Total number of orders created")
                .tag("type", "order")
                .register(registry);

        this.flakyErrors = Counter.builder("business.flaky.errors.total")
                .description("Total number of intentional flaky errors triggered")
                .register(registry);

        this.slowRequests = Counter.builder("business.slow.requests.total")
                .description("Total number of slow endpoint hits")
                .register(registry);

        Gauge.builder("business.active.users", activeUsers, AtomicInteger::get)
                .description("Current number of active (in-flight) simulated users")
                .register(registry);

        Gauge.builder("business.last.slow.response.ms", lastSlowResponseMs, AtomicLong::get)
                .description("Last recorded slow response duration in milliseconds")
                .register(registry);

        this.orderProcessingTimer = Timer.builder("business.order.processing.duration")
                .description("Time spent processing an order (simulated)")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void incrementOrders() {
        ordersCreated.increment();
    }

    public void incrementFlakyErrors() {
        flakyErrors.increment();
    }

    public void incrementSlowRequests() {
        slowRequests.increment();
    }

    public void setActiveUsers(int count) {
        activeUsers.set(count);
    }

    public void recordSlowResponseMs(long ms) {
        lastSlowResponseMs.set(ms);
    }

}