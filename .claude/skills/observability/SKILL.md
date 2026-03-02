---
name: observability
description: Load when adding logging, metrics, monitoring, MDC context, or working with Prometheus/Grafana observability infrastructure.
---

# Observability Guide

Load this context when adding logging, metrics, or monitoring.

---

## Logging

### MDC (Mapped Diagnostic Context)

Request tracking with correlation IDs:

```kotlin
// src/main/kotlin/com/neki/common/filter/RequestMdcFilter.kt
class RequestMdcFilter : OncePerRequestFilter() {
    override fun doFilterInternal(...) {
        MDC.put("requestId", UUID.randomUUID().toString())
        MDC.put("uri", request.requestURI)
        // ...
    }
}
```

### Auth MDC

User context in logs:

```kotlin
// src/main/kotlin/com/neki/auth/infra/security/filter/AuthMdcFilter.kt
class AuthMdcFilter : OncePerRequestFilter() {
    override fun doFilterInternal(...) {
        MDC.put("userId", authentication.id)
        // ...
    }
}
```

### Logstash Encoder

JSON formatted logs for ELK stack:

```xml
<!-- logback-spring.xml -->
<encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
```

---

## Metrics

### Spring Boot Actuator

Health checks and metrics endpoint:

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health, metrics, prometheus
  endpoint:
    health:
      show-details: always
```

Endpoints:

- `/actuator/health` - Health check
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus format metrics

---

## Monitoring Stack

**Single Source of Truth**: `infra/monitoring/`

| Tool       | Purpose            |
|------------|--------------------|
| Prometheus | Metrics collection |
| Grafana    | Dashboards         |

### Important Note

Prometheus also collects **load-test metrics**. Do not modify the monitoring setup without
considering load testing impact.

---

## Rules

- **Do NOT remove** observability code (logging, MDC, metrics)
- **Do NOT bypass** existing observability filters
- Changes to monitoring require review of `infra/monitoring/`
- All new endpoints should include proper logging
