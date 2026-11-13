# AMC Bank — Full Stack Banking Application

AMC Bank is a full-stack banking application that covers the complete request path from a React single-page frontend through a Spring Security JWT filter to JPA-managed persistence. Admins can manage customers, open accounts, process deposits, withdrawals and transfers, sanction loans with EMI calculation, view statements, monitor fraud alerts and check a customer's financial health. Customers get self-service access to their own profile, accounts, loans, transactions, fixed deposits and password. Demo data is seeded on first run so the app is ready to use immediately, with H2 in-memory as the default database and an optional PostgreSQL profile.

## Tech Stack

| Layer     | Technology |
|-----------|------------|
| Backend   | Java 17+, Spring Boot 3.3.4, Spring Web, Spring Data JPA, Spring Security, JJWT 0.12.6 |
| Database  | H2 in-memory (default), PostgreSQL (optional profile) |
| Frontend  | React 19, Vite 8, plain CSS |
| Build     | Maven (wrapper included), npm |

## Project Structure

```
amc-bank-fullstack/
├── amc-backend/        # Spring Boot REST API (port 8080)
│   ├── src/main/java/com/amcbank/simple/
│   │   ├── controller/     # BankController (all /api routes)
│   │   ├── security/       # JWT filter, JwtService, SecurityConfig (CORS, roles)
│   │   ├── service/        # BankService, ForexService, FraudDetectionService, ...
│   │   ├── model/          # JPA entities
│   │   ├── repository/     # Spring Data repositories
│   │   └── dto/            # Request/response records
│   ├── src/main/resources/ # application.properties, H2/Postgres profiles
│   ├── mvnw / mvnw.cmd     # Maven wrapper
│   └── pom.xml
└── amc-frontend/       # React + Vite single-page app (port 5173)
    ├── src/App.jsx         # UI + API calls (http://localhost:8080/api)
    ├── src/main.jsx
    ├── index.html
    └── package.json
```

## Prerequisites

- JDK 17 or newer (`java -version`)
- Node.js 20+ and npm
- Maven is **not** required — the included `mvnw` wrapper downloads it
- PostgreSQL only if you want to use the Postgres profile (the app runs on H2 out of the box)

## Quick Start

### 1. Backend (port 8080)

```bash
cd amc-backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

The API starts at `http://localhost:8080`. The H2 console is available at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:amc_bank`, user `sa`, empty password).

On first start the app seeds demo data:

| Role     | Username          | Password    |
|----------|-------------------|-------------|
| Admin    | `admin`           | `admin123`  |
| Customer | `rahul@gmail.com` | `password123` |

> Data is in-memory and resets on every restart. Demo balances: Savings `AC10012026` (₹75,000) and Current `AC20022026` (₹25,000).

### 2. Frontend (port 5173)

Open a second terminal:

```bash
cd amc-frontend
npm install
npm run dev
```

Open `http://localhost:5173` and sign in with one of the demo accounts above.

> The frontend calls `http://localhost:8080/api` (configured in `src/App.jsx`), so start the backend first. CORS on the backend allows `localhost` and `127.0.0.1` on any port.

## Features

**Admin**
- Dashboard summary (customers, accounts, loans)
- Customer CRUD and search
- Open accounts, deposit, withdraw, transfer
- Transaction history and account statements
- Loan listing and sanctioning with EMI calculation
- Fixed deposits
- Fraud alerts and risk evaluation
- Per-customer financial health score

**Customer**
- Self-service profile (`/api/customer/me`)
- View own accounts, loans, transactions and statements
- Deposit / withdraw / transfer on own accounts
- Open, list and close fixed deposits
- Financial health score
- Change password

**Public**
- `POST /api/login` — returns a JWT
- `GET /api/forex/rates`, `POST /api/forex/convert`

## API Overview

| Method | Path | Access |
|--------|------|--------|
| POST | `/api/login` | Public |
| GET | `/api/auth/me` | Any authenticated role |
| GET | `/api/dashboard` | Admin |
| GET/POST/PUT/DELETE | `/api/customers/**` | Admin |
| POST | `/api/accounts/{id}/deposit`, `/withdraw` | Admin |
| POST | `/api/accounts/transfer` | Admin |
| GET | `/api/transactions`, `/api/accounts/{id}/transactions` | Admin |
| GET | `/api/accounts/{id}/statement` | Admin |
| GET/POST | `/api/customers/{id}/loans` | Admin |
| GET/POST | `/api/fixed-deposits` | Admin |
| GET | `/api/security/fraud-alerts`, `POST /api/security/evaluate-risk` | Admin |
| GET | `/api/customers/{id}/financial-health` | Admin |
| GET | `/api/customer/me`, `/me/accounts`, `/me/loans`, `/me/transactions` | Customer |
| POST | `/api/customer/me/accounts/{id}/deposit`, `/withdraw`, `/me/transfer` | Customer |
| POST | `/api/customer/me/password` | Customer |
| GET | `/api/forex/rates` | Public |

Protected routes require the header `Authorization: Bearer <token>`.

## Configuration

`amc-backend/src/main/resources/application.properties`:

- `server.port=8080`
- **H2** in-memory database by default
- `app.jwt.secret` — defaults to a classroom secret; override with the `JWT_SECRET` environment variable
- `app.jwt.expiration-ms=3600000` (1 hour)

### Using PostgreSQL

```bash
cd amc-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

The `postgres` profile expects a database `amc_bank` on `localhost:5432` with user `postgres` and password `1234` (override with `PG_PASSWORD`). Create the database first:

```sql
CREATE DATABASE amc_bank;
```

## Push to GitHub

The repository already includes a `.gitignore` for build output and dependencies.

```bash
git init
git add .
git commit -m "Initial commit: AMC Bank full-stack app"
git branch -M main
git remote add origin https://github.com/<your-username>/<your-repo>.git
git push -u origin main
```

## Security Note

This project is intended for classroom learning. The default JWT secret and seeded credentials are **not** production-safe. For real deployments use strong secrets, hashed bootstrap credentials, HTTPS, token rotation/revocation, auditing, database migrations and proper access controls.
