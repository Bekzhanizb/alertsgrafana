# observability-demo

Spring Boot 3 + Java 21 сервис для практики по наблюдаемости: приложение, метрики, Prometheus, Grafana, Alertmanager и runbook.

## Что реализовано

- Spring Boot REST API с health-check и деградационными endpoint'ами
- Spring Boot Actuator + Micrometer Prometheus
- Prometheus scrape и alert rules
- Grafana datasource и auto-provisioned dashboard
- Alertmanager с маршрутизацией по `severity`
- Runbook с метриками, порогами и действиями при алертах

## Структура

```text
.
├── docker-compose.yml
├── monitoring/
│   ├── alertmanager/alertmanager.yml
│   ├── grafana/provisioning/
│   │   ├── dashboards/
│   │   │   ├── dashboard-provider.yml
│   │   │   └── sre-dashboard.json
│   │   └── datasources/prometheus.yml
│   └── prometheus/
│       ├── alerts.yml
│       └── prometheus.yml
├── runbook.md
└── src/
    ├── main/java/io/bekezhan/alertsgrafana/
    └── main/resources/application.yaml
```

## API

| Method | URL | Назначение |
|---|---|---|
| GET | `/api/health` | прикладной health-check |
| GET | `/api/orders` | тестовый бизнес endpoint |
| POST | `/api/orders` | создание заказа + кастомная метрика |
| GET | `/api/slow` | искусственная задержка для latency alert |
| GET | `/api/flaky` | искусственные 500 ошибки для error-rate alert |
| GET | `/api/stress/cpu?seconds=20` | CPU pressure для resource alert |
| GET | `/api/stress/memory?mb=96&holdSeconds=30` | heap pressure для resource alert |
| GET | `/actuator/health` | health от Spring Boot Actuator |
| GET | `/actuator/prometheus` | метрики для Prometheus |

## Быстрый старт

```bash
docker compose up -d --build
```

Доступ:

- App: `http://localhost:8080`
- Prometheus: `http://localhost:9090`
- Alertmanager: `http://localhost:9093`
- Grafana: `http://localhost:3000` (`admin/admin`)

Grafana dashboard provisioned автоматически:

- `Dashboards -> SRE -> Observability Demo - SRE Dashboard`

## Проверка требований задания

- REST API: выполнено
- health endpoint: выполнено
- endpoint с задержкой/ошибкой: выполнено
- `/actuator/prometheus`: выполнено
- HTTP/JVM/CPU/latency метрики: выполнено
- Prometheus + Grafana: настроены
- dashboard с availability/RPS/error rate/p95/p99/CPU/memory/threads: настроен
- минимум 4 alert rule с `for` и severity: настроено 5 правил
- runbook: добавлен в [runbook.md](/home/bekezhan/IdeaProjects/alertsGrafana/runbook.md)

## Как вызвать деградацию

```bash
# latency
watch -n 0.5 curl -s http://localhost:8080/api/slow

# error rate
for i in $(seq 1 200); do curl -s http://localhost:8080/api/flaky > /dev/null; done

# cpu
curl "http://localhost:8080/api/stress/cpu?seconds=240"

# memory
curl "http://localhost:8080/api/stress/memory?mb=96&holdSeconds=300"
```
