# Runbook: Observability Demo

## Purpose

This repository demonstrates a baseline SRE observability stack for a Spring Boot service:

- application metrics via Spring Boot Actuator and Micrometer
- monitoring via Prometheus
- visualization via Grafana
- alert routing via Alertmanager

## Service Endpoints

- `GET /api/health` - lightweight application health check
- `GET /api/orders` - normal business endpoint
- `POST /api/orders` - creates a demo order and increments business metrics
- `GET /api/slow` - intentionally slow endpoint for latency alert validation
- `GET /api/flaky` - intentionally returns intermittent 500 responses
- `GET /api/stress/cpu?seconds=20` - generates CPU pressure for alert testing
- `GET /api/stress/memory?mb=96&holdSeconds=30` - generates heap pressure for alert testing
- `GET /actuator/health` - actuator health endpoint
- `GET /actuator/prometheus` - Prometheus metrics endpoint

## Metrics

### Platform Metrics

- `http_server_requests_seconds_*` - request throughput, latency, status-code distribution
- `jvm_memory_used_bytes`, `jvm_memory_max_bytes` - JVM heap usage
- `jvm_threads_live_threads` - live thread count
- `process_cpu_usage` - process CPU utilization
- `system_cpu_usage` - host-level CPU utilization when available
- `up` - Prometheus scrape availability

### Custom Business Metrics

- `business.orders.created.total` - total created demo orders
- `business.flaky.errors.total` - total intentional flaky failures
- `business.slow.requests.total` - total slow endpoint hits
- `business.active.users` - simulated active users gauge
- `business.last.slow.response.ms` - latest slow response duration
- `business.order.processing.duration` - timer for demo order creation

## Grafana Dashboard

Dashboard: `Observability Demo - SRE Dashboard`

Panels:

- availability
- RPS
- error rate
- latency p95/p99
- CPU usage
- JVM heap usage
- active threads
- request rate by URI
- heap bytes used vs max

## Alert Rules

### 1. InstanceDown

- Expression: `up{job="observability-demo"} == 0`
- Severity: `critical`
- `for`: `1m`
- Rationale: one missed scrape should not page, but a full minute means the service is unavailable or not reachable.
- Action:
  - check `docker ps`
  - check application logs
  - check `/actuator/health`
  - confirm network reachability from Prometheus

### 2. HighErrorRate

- Expression: 5xx rate over total request rate for the last 5 minutes
- Severity: `critical`
- `for`: `5m`
- Threshold: `> 10%`
- Rationale: this is high enough to avoid noise from brief failures and low enough to catch a degraded API quickly.
- Action:
  - inspect `/api/flaky` traffic pattern
  - inspect recent deploy/config changes
  - review application logs for stack traces
  - compare with latency and CPU panels

### 3. HighLatencyP95

- Expression: p95 request latency from `http_server_requests_seconds_bucket`
- Severity: `warning`
- `for`: `5m`
- Threshold: `> 1.0s`
- Rationale: `/api/slow` deliberately exceeds this value, so the alert is easy to validate while still representing meaningful user-facing slowness.
- Action:
  - identify hot URI in the RPS-by-URI panel
  - inspect thread count and CPU
  - determine whether slowness is synthetic test traffic or a genuine regression

### 4. HighCpuUsage

- Expression: `process_cpu_usage > 0.80`
- Severity: `warning`
- `for`: `3m`
- Rationale: sustained CPU above 80% is a realistic early-warning threshold; the `for` clause prevents flapping from short bursts.
- Action:
  - confirm whether `/api/stress/cpu` is running
  - inspect hot endpoints and request rate
  - capture thread dump if unexplained

### 5. HighJvmMemoryUsage

- Expression: heap used / heap max `> 0.85`
- Severity: `warning`
- `for`: `5m`
- Rationale: 85% sustained heap use indicates elevated GC pressure but leaves headroom before OOM.
- Action:
  - confirm whether `/api/stress/memory` is under test
  - inspect heap panel and GC behavior
  - reduce load or restart the container if memory does not recover

## Why the Thresholds Look This Way

- All alerts use `for` to reduce flapping and avoid alerting on short spikes.
- `critical` is reserved for availability loss and sustained high 5xx rate.
- `warning` is used for latency and resource saturation because these often need investigation before escalation.
- Thresholds are tuned for a demo system with predictable synthetic degradation endpoints.

## Validation Steps

### Validate Metrics

```bash
curl http://localhost:8080/actuator/prometheus | head
```

### Validate Latency Alert

```bash
watch -n 0.5 curl -s http://localhost:8080/api/slow
```

### Validate Error Alert

```bash
for i in $(seq 1 200); do curl -s http://localhost:8080/api/flaky > /dev/null; done
```

### Validate CPU Alert

```bash
curl "http://localhost:8080/api/stress/cpu?seconds=240"
```

### Validate Memory Alert

```bash
curl "http://localhost:8080/api/stress/memory?mb=96&holdSeconds=300"
```

## Evidence to Attach

- repository link
- Grafana dashboard screenshots
- Prometheus or Alertmanager alert screenshots
- optional curl output for `/actuator/prometheus`
