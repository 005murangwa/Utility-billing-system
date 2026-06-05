# Utility Billing System

Production-grade Spring Boot 3 backend for a Utility Billing System.

## Tech Stack

- Java 21
- Spring Boot 3.2
- PostgreSQL
- Spring Data JPA
- Spring Security + JWT
- Flyway Migrations
- Swagger OpenAPI (springdoc)
- Lombok

## Prerequisites

- JDK 21
- Maven 3.9+
- PostgreSQL 14+

## Database Setup

```sql
CREATE DATABASE ubs_db;
CREATE USER ubs_user WITH ENCRYPTED PASSWORD 'ubs_password';
GRANT ALL PRIVILEGES ON DATABASE ubs_db TO ubs_user;
```

## Configuration

Update `src/main/resources/application.yml` or set environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `JWT_SECRET` | 256-bit hex or Base64 secret | built-in dev key |
| `JWT_EXPIRATION_MS` | Token expiration in ms | `86400000` |

## Run

```bash
# Default profile
./mvnw spring-boot:run

# Local overrides (PostgreSQL on 5433, port 8081)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Copy `application-local.properties.example` to `application-local.properties` for local DB/mail settings.

API base URL: `http://localhost:8080/api` (or `8081` with `local` profile)

Swagger UI: `http://localhost:8080/api/swagger-ui.html`

## Default Seeded Data

**Roles:** `ROLE_ADMIN`, `ROLE_OPERATOR`, `ROLE_FINANCE`, `ROLE_CUSTOMER`

**Default Admin (pre-verified):**
- Email: `brillanteigabemurangwa@gmail.com`
- Username: `brillanteigabemurangwa`
- Password: `Password123!`

## Authentication Flow

1. **Register** — `POST /api/auth/register` with `fullName`, `email`, `phoneNumber`, `password`
2. **Verify OTP** — `POST /api/auth/verify-otp` with email + 6-digit OTP from email
3. **Login** — `POST /api/auth/login` (only verified users)
4. **Refresh** — `POST /api/auth/refresh` with `refreshToken`
5. **Logout** — `POST /api/auth/logout` with Bearer access token (optionally revoke refresh token)

## Password Recovery Flow

1. **Forgot Password** — `POST /api/auth/password/forgot` with email (sends OTP)
2. **Verify Reset OTP** — `POST /api/auth/password/verify-otp` with email + OTP (returns `resetToken`)
3. **Reset Password** — `POST /api/auth/password/reset` with `resetToken` + `newPassword`

## API Endpoints

### Public Auth

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register and send verification OTP |
| POST | `/api/auth/verify-otp` | Verify email OTP and activate account |
| POST | `/api/auth/resend-otp` | Resend verification OTP |
| POST | `/api/auth/login` | Login (verified users only) |
| POST | `/api/auth/refresh` | Refresh access token |
| POST | `/api/auth/password/forgot` | Request password reset OTP |
| POST | `/api/auth/password/verify-otp` | Verify reset OTP, get reset token |
| POST | `/api/auth/password/reset` | Reset password with reset token |

### Secured Auth

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/logout` | Blacklist JWT and revoke refresh token |
| POST | `/api/auth/change-temporary-password` | First-login password change for staff accounts |

### Staff Provisioning (Admin only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users/staff` | Create staff account with temporary password email |
| PUT | `/api/users/{id}/roles` | Assign/update roles with email notification |

Staff login returns `passwordChangeRequired=true` until the temporary password is changed.

### Secured (Bearer Token)

| Method | Endpoint | Access |
|--------|----------|--------|
| GET | `/api/users/me` | Authenticated user |
| GET | `/api/users` | Admin |
| GET | `/api/users/{id}` | Admin |
| GET | `/api/roles` | Admin |

### Customers (Admin only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/customers` | Create customer |
| PUT | `/api/customers/{id}` | Update customer |
| DELETE | `/api/customers/{id}` | Delete customer |
| GET | `/api/customers/{id}` | Get customer by ID |
| GET | `/api/customers` | List customers (pagination, sorting, search) |

### Meters (Admin and Operator)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/meters` | Create meter |
| PUT | `/api/meters/{id}` | Update meter |
| DELETE | `/api/meters/{id}` | Delete meter |
| GET | `/api/meters/{id}` | View meter by ID |
| GET | `/api/meters` | List meters (pagination, sorting, search) |
| GET | `/api/meters/customer/{customerId}` | View all meters for a customer |

### Meter Readings (Operator only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/meter-readings` | Create meter reading |
| PUT | `/api/meter-readings/{id}` | Update meter reading |
| DELETE | `/api/meter-readings/{id}` | Delete meter reading |
| GET | `/api/meter-readings/{id}` | Get reading by ID |
| GET | `/api/meter-readings` | List readings (pagination, sorting, filters) |

### Tariffs (Admin only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tariffs` | Create tariff (auto-versioned) |
| PUT | `/api/tariffs/{id}` | Create new tariff version (history preserved) |
| PATCH | `/api/tariffs/{id}/deactivate` | Deactivate tariff |
| GET | `/api/tariffs/{id}` | View tariff by ID |
| GET | `/api/tariffs/active/{meterType}` | Latest active tariff for billing |
| GET | `/api/tariffs` | List tariffs (pagination, sorting, filters) |

### Bills (Admin and Finance)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/bills/generate` | Generate bill from meter reading |
| PATCH | `/api/bills/{id}/approve` | Approve pending bill |
| GET | `/api/bills/{id}` | Get bill by ID |
| GET | `/api/bills` | List bills (pagination, sorting, filters) |
| GET | `/api/bills/customer/{customerId}` | Get all bills for a customer |

### Payments (Finance only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payments` | Create payment (partial or full) |
| GET | `/api/payments/{id}` | Get payment by ID |
| GET | `/api/payments` | List payments (pagination, filters) |
| GET | `/api/payments/customer/{customerId}` | Customer payment history |

### Notifications (Customer only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/notifications` | Get notifications for authenticated customer |
| GET | `/api/notifications/{id}` | Get notification by ID |
| GET | `/api/notifications/customer/{customerId}` | Customer notifications (own profile only) |

Notifications are created for bill generation, bill approval, full payment, overdue penalties, and meter disconnection. Duplicate notifications are prevented by event type and reference ID.

PostgreSQL triggers also insert notifications when bills are approved or fully paid.

### Customer Portal (Customer only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/my/bills` | View own approved bills |
| GET | `/api/my/bills/{id}` | View own approved bill |
| GET | `/api/my/payments` | View own payment history |

### Comments (Admin, Finance, Operator, Customer)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/comments` | Create bill comment |
| GET | `/api/comments/{id}` | Get comment by ID |
| GET | `/api/comments` | List comments (pagination, search) |
| GET | `/api/comments/bill/{billId}` | Comments for a bill |

### Audit Logs (Admin, Finance, Operator)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/audit-logs/{id}` | Get audit log by ID |
| GET | `/api/audit-logs` | Search audit logs with old/new values |

Tracked actions: `CREATE`, `UPDATE`, `DELETE`, `APPROVE` across users, customers, meters, readings, tariffs, bills, and payments.

### Billing Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `BILLING_DUE_DAYS` | Days after approval until due date | `30` |
| `BILLING_OVERDUE_DISCONNECT_DAYS` | Days overdue before meter disconnection | `60` |
| `BILLING_OVERDUE_CRON` | Scheduled overdue processing cron | `0 0 1 * * *` |

## Validation Rules

- **Email:** lowercase, valid format, unique
- **Phone:** Rwanda format (`+2507XXXXXXXX` / `07XXXXXXXX`), unique
- **Password:** min 8 chars, uppercase, lowercase, digit, special character

## Authentication

1. Register and verify OTP, or login as seeded admin
2. Copy `accessToken` from login response
3. In Swagger, click **Authorize** and enter: `Bearer <token>`

## Project Structure

```
com.ubs.billing
├── config          # Security, Swagger, Data seeding
├── controller      # REST controllers
├── dto             # Request/Response DTOs and mappers
├── entity          # JPA entities
├── exception       # Global exception handling
├── repository      # Spring Data repositories
├── security        # JWT filter, UserDetails, JwtService
├── service         # Business logic
└── util            # ApiResponse, constants
```
