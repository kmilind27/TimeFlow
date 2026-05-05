# TimeFlow - Timesheet & Leave Management System

TimeFlow is an enterprise-grade workforce management application designed to handle employee timesheets, leave requests, and administrative workflows. The project uses a modern microservices architecture on the backend and a responsive, component-based frontend.

## 🌟 Key Features

- **Role-Based Access Control**: Secure JWT-based authentication supporting Employee, Manager, and Admin roles.
- **Timesheet Management**: Log work hours, track weekly timesheets, and manage project assignments.
- **Leave Management**: Apply for leave, check leave balances, and track holiday schedules.
- **Approval Workflows**: Dedicated interfaces for Managers to review and approve/reject timesheets and leave requests.
- **Admin Dashboard**: System-wide overview, user management, and aggregated reporting.
- **Event-Driven Notifications**: Asynchronous email notifications powered by RabbitMQ for system events and updates.
- **Secure Password Reset**: 3-step OTP-based forgot password flow.

## 🏗️ Architecture & Tech Stack

The application is split into two main components: a React frontend and a Spring Boot microservices backend.

### Frontend
- **Framework**: React 19 + Vite
- **Routing**: React Router v7
- **Styling**: CSS Modules
- **HTTP Client**: Axios

### Backend
- **Core**: Spring Boot 4.x Microservices (Java 17)
- **Database**: MySQL 8.0 + JPA/Hibernate
- **Security**: Spring Security + JWT (HMAC-SHA256)
- **API Gateway**: Spring Cloud Gateway (WebFlux)
- **Service Discovery**: Netflix Eureka
- **Messaging**: RabbitMQ (Topic Exchanges)
- **Inter-Service Communication**: OpenFeign with Fallbacks
- **Observability**: Zipkin + Micrometer for distributed tracing
- **API Documentation**: SpringDoc OpenAPI 3 (Swagger UI)

### Microservices
| Service | Port | Role |
|---------|------|------|
| **Eureka Server** | 8761 | Service registry and discovery |
| **API Gateway** | 8080 | Central entry point, routing, CORS, JWT validation |
| **Auth Service** | 8081 | Authentication, user management, OTP password reset |
| **Timesheet Service** | 8082 | Weekly timesheets, time entries, manager reviews |
| **Leave Service** | 8083 | Leave requests, balances, holidays, manager reviews |
| **Admin Service** | 8084 | Admin dashboard, reports, data aggregation via Feign |
| **Notification Service**| 8085 | Listens to RabbitMQ queues and sends SMTP emails |

## 🚀 Getting Started

You can run the entire application using Docker Compose, or run the frontend and backend separately for local development.

### Option 1: Docker (Recommended)

1. Ensure Docker and Docker Compose are installed.
2. Navigate to the `Backend` directory and build/start the containers:
   ```bash
   cd Backend
   docker compose up --build -d
   ```
3. The API Gateway will be available at `http://localhost:8080` and the frontend can be configured to point to it.

### Option 2: Local Development

#### Prerequisites
- Node.js (v18+)
- Java 17
- MySQL 8.x
- Maven 3.x
- RabbitMQ

#### 1. Start the Backend
Start the microservices in the following order:
1. **Eureka Server** (`http://localhost:8761`)
2. **Auth Service** (`http://localhost:8081`)
3. **Other Services** (Timesheet, Leave, Admin, Notification)
4. **API Gateway** (`http://localhost:8080`)

To start a service:
```bash
cd Backend/<service-directory>
./mvnw spring-boot:run
```

#### 2. Start the Frontend
In a new terminal:
```bash
cd Frontend
npm install
npm run dev
```
The frontend will be available at `http://localhost:5173`.

## 📂 Project Structure

```text
Timesheet-System/
├── Backend/                 # Spring Boot Microservices
│   ├── admin-service/       # Admin reporting & dashboard
│   ├── api-gateway/         # Edge service & routing
│   ├── auth-service/        # JWT & User Auth
│   ├── eureka-server/       # Service registry
│   ├── leave-service/       # Leave management
│   ├── notification-service/# Email notifications (RabbitMQ)
│   ├── timesheet-service/   # Timesheet management
│   ├── docker-compose.yml   # Full backend stack deployment
│   └── pom.xml              # Maven Aggregator POM
└── Frontend/                # React Vite Application
    ├── src/
    │   ├── api/             # Axios API calls
    │   ├── components/      # Reusable UI components
    │   ├── context/         # React context (Auth)
    │   └── pages/           # Role-based views (Admin, Manager, Employee)
    ├── package.json
    └── vite.config.js
```

## 🛠️ Code Quality
The project utilizes **SonarQube** and **JaCoCo** for unified code quality analysis and test coverage reporting across all microservices via the root Maven aggregator POM.

```bash
# Run SonarQube analysis from the Backend directory
cd Backend
mvn clean verify sonar:sonar
```
