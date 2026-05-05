# Timesheet & Leave Management System

Enterprise-grade workforce management application built with Spring Boot Microservices.

## Tech Stack
- **Backend**: Spring Boot 4.x Microservices
- **Security**: Spring Security + JWT (HMAC-SHA256)
- **Gateway**: Spring Cloud Gateway (WebFlux)
- **Registry**: Netflix Eureka
- **Database**: MySQL 8.0 + JPA/Hibernate
- **Messaging**: RabbitMQ (Topic Exchanges)
- **Inter-Service**: OpenFeign with Fallbacks
- **Tracing**: Zipkin + Micrometer
- **API Docs**: SpringDoc OpenAPI 3 (Swagger UI)
- **Containerization**: Docker + Docker Compose
- **Code Quality**: SonarQube + JaCoCo
- **Java**: 17

## Architecture
```
              React Frontend (:3000)
                     ↓
              API Gateway (:8080)
              JWT + CORS + Routing
                     ↓  (Eureka lb://)
  ┌──────────────────────────────────────────────┐
  │  Auth     Timesheet    Leave    Admin         │
  │  :8081     :8082       :8083    :8084         │
  └──────────────────────────────────────────────┘
       │          │           │          │
    MySQL     RabbitMQ ──► Notification Service (:8085) ──► SMTP
                           Zipkin (:9411)
                           Eureka (:8761)
```

## Services

| Service | Port | Role |
|---------|------|------|
| Eureka Server | 8761 | Service discovery |
| API Gateway | 8080 | Routing, JWT validation, CORS |
| Auth Service | 8081 | Signup, login, JWT, OTP password reset, user management |
| Timesheet Service | 8082 | Weekly timesheets, time entries, manager reviews |
| Leave Service | 8083 | Leave requests, balances, holidays, manager reviews |
| Admin Service | 8084 | Dashboard, reports, user/holiday management (via Feign) |
| Notification Service | 8085 | Email notifications consumed from RabbitMQ |

## Key Features
- **JWT Authentication** with role-based access (EMPLOYEE / MANAGER / ADMIN)
- **3-Step OTP Forgot Password** flow via RabbitMQ + email
- **Event-Driven Notifications** — 10 RabbitMQ queues for user, timesheet, and leave events
- **Manager Approval Workflows** for both timesheets and leave requests
- **Admin Dashboard** with aggregated stats from all services via OpenFeign
- **Distributed Tracing** with Zipkin (100% sampling)
- **Full Docker Compose** setup with health checks for all services

## How to Run

### Docker (Recommended)
```bash
cd Backend
docker compose up --build -d
```

### Local Development
**Prerequisites**: Java 17, MySQL 8.x, Maven 3.x, RabbitMQ

**Start Order**:
1. Eureka Server → http://localhost:8761
2. Auth Service → http://localhost:8081
3. Other services (any order)
4. API Gateway → http://localhost:8080

```bash
cd <service-directory>
./mvnw spring-boot:run
```

## Databases
Auto-created by Hibernate (`ddl-auto: update`):
- `authdb` — users, credentials, roles
- `timesheet_db` — projects, timesheets, time entries
- `leave_db` — leave requests, balances, holidays
- `admin_db` — admin service data

## Progress
- [x] Phase 1 — Architecture Design
- [x] Phase 2 — Auth Service + JWT
- [x] Phase 3 — API Gateway
- [x] Phase 4 — Timesheet Service
- [x] Phase 5 — Leave Service
- [x] Phase 6 — Admin Service
- [x] Phase 7 — Notification Service + RabbitMQ
- [x] Phase 8 — OTP Forgot Password
- [x] Phase 9 — Docker Compose + Deployment
