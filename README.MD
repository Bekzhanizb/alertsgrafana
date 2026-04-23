# observability-demo

Spring Boot 3 + Java 21 сервис для SRE-практики по наблюдаемости.

## Структура

```
observability-demo/
├── src/
│   └── main/java/kz/qazcode/observability/
│       ├── ObservabilityDemoApplication.java
│       ├── controller/DemoController.java      # REST API
│       ├── service/OrderService.java           # бизнес-логика + деградация
│       ├── metrics/BusinessMetrics.java        # кастомные метрики
│       └── config/MetricsConfig.java           # TimedAspect
├── src/main/resources/application.yml
├── Dockerfile
├── monitoring/
│   ├── docker-compose.yml
│   ├── prometheus/
│   │   ├── prometheus.yml
│   │   └── alerts.yml                          # 5 алертов + bonus
│   ├── alertmanager/alertmanager.yml
│   └── grafana/provisioning/
│       ├── datasources/prometheus.yml
│       └── dashboards/sre-dashboard.json       # auto-provisioned dashboard
└── runbook.md
```

## Endpoints

| Method | URL | Описание |
|--------|-----|----------|
| GET | `/api/health` | Health-check приложения |
| GET | `/api/orders` | Список заказов (stub) |
| POST | `/api/orders` | Создать заказ (business metric) |
| GET | `/api/slow` | Искусственная задержка 2s (тест HighLatency) |
| GET | `/api/flaky` | 50% вероятность 500 (тест HighErrorRate) |
| GET | `/actuator/prometheus` | Метрики для Prometheus |
| GET | `/actuator/health` | Actuator health |

## Быстрый старт

### 1. Только сервис (локально)

```bash
./mvnw spring-boot:run
# или
mvn spring-boot:run

# Проверить метрики
curl http://localhost:8080/actuator/prometheus | head -40
```

### 2. Полный стек (сервис + Prometheus + Grafana)

```bash
cd monitoring
docker compose up -d

# Сервис:    http://localhost:8080
# Prometheus: http://localhost:9090
# Grafana:    http://localhost:3000  (admin/admin)
# Alertmanager: http://localhost:9093
```

Dashboard уже добавлен автоматически через provisioning:
`Dashboards → SRE → Observability Demo — SRE Dashboard`

## Тест деградации

```bash
# Trigger HighLatency
watch -n 0.5 curl -s http://localhost:8080/api/slow

# Trigger HighErrorRate
for i in $(seq 1 100); do curl -s http://localhost:8080/api/flaky; done

# Нагрузочный тест (нужен hey: go install github.com/rakyll/hey@latest)
hey -n 1000 -c 20 http://localhost:8080/api/flaky
```

## Конфигурация деградации

В `application.yml`:

```yaml
app:
  slow-endpoint:
    delay-ms: 2000      # задержка /api/slow в мс
  error-endpoint:
    error-rate: 0.5     # вероятность 500 на /api/flaky (0.0–1.0)
```