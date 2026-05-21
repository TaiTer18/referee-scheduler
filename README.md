# Referee Scheduler

A referee scheduling application for managing soccer games, referee availability, and assignments. The current codebase includes a Spring Boot backend with JWT authentication, refresh-token rotation, scheduling endpoints for admins and referees, and integration test coverage for the main API flows.

## Author

Nicholas Tait

## Tech Stack

- Backend: Spring Boot 4, Java 25, Maven
- Security: Spring Security, JWT, refresh-token rotation
- Database: PostgreSQL
- Testing: JUnit 5, MockMvc, H2
- Frontend: ReactTS + Vite

## Current Backend Features

- User registration and login for admins and referees
- Short-lived JWT access tokens
- Refresh tokens stored hashed in the database
- Refresh-token rotation on `/api/auth/refresh`
- Logout by revoking refresh tokens
- Admin game CRUD
- Referee availability submission
- Referee assignment and unassignment
- Role-based access control for admin vs referee routes
- Integration tests for auth and scheduling flows

## Project Structure

- [backend](backend): Spring Boot API
- [database/schema.sql](database/schema.sql): PostgreSQL schema
- [docs/architecture.md](docs/architecture.md): system structure and request flow
- [docs/decisions.md](docs/decisions.md): key technical decisions and tradeoffs
- [docs/interview-notes.md](docs/interview-notes.md): interview-ready explanation notes

## Quick Start

### Prerequisites

- Java 25
- Maven
- PostgreSQL running locally

### 1. Create the database

```bash
createdb referee_scheduler
```

### 2. Create the schema

```bash
psql -U postgres -d referee_scheduler -f database/schema.sql
```

### 3. Configure backend environment variables

The backend reads its local development settings from [`backend/.env`](backend/.env).

Important values:

- `JDBC_DATABASE_URL`
- `JDBC_DATABASE_USERNAME`
- `JDBC_DATABASE_PASSWORD`
- `JPA_DDL_AUTO`
- `APP_JWT_EXPIRATION_MS`

### 4. Start the backend

```bash
cd backend
mvn spring-boot:run
```

### 5. Run tests

```bash
cd backend
mvn test
```

## Main API Areas

### Authentication

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `POST /api/auth/refresh`
- `GET /api/auth/me`

### Referee

- `GET /api/referees/available-games`
- `POST /api/referees/availability`
- `GET /api/referees/{id}/availability`
- `GET /api/referees/{id}/assignments`

### Admin

- `GET /api/admin/games`
- `POST /api/admin/games`
- `PUT /api/admin/games/{id}`
- `DELETE /api/admin/games/{id}`
- `GET /api/admin/games/{id}/referees`
- `POST /api/admin/assignments`
- `DELETE /api/admin/assignments/{id}`
- `GET /api/admin/referees`
- `GET /api/admin/referees/{id}`

## Data Model

Current main tables:

- `users`
- `games`
- `referee_availability`
- `game_assignments`
- `refresh_tokens`

## Notes

- Access tokens currently expire after 15 minutes.
- Refresh tokens are rotated on every successful refresh.
- The frontend is not built yet, but the backend API is designed to support a TypeScript client cleanly.

## License

MIT License - see `LICENSE`
