# Referee Scheduler

A full-stack web application for managing soccer referee scheduling. Referees submit their availability for games, and admins assign referees based on who's available.

## Author
Nicholas Tait 

## Tech Stack
- **Backend:** Spring Boot (Java)
- **Frontend:** React + TypeScript
- **Database:** PostgreSQL
- **Testing:** JUnit (backend), Vitest (frontend)
- **Deployment:** TBD

## Quick Start
1. Create the PostgreSQL database and tables.
2. Configure backend environment variables.
3. Start the Spring Boot backend.
4. Start the frontend.

### Backend Setup
Prerequisites:
- Java 17
- Maven
- PostgreSQL running locally

1. Create the database:

```bash
createdb referee_scheduler
```

2. Create the schema:

```bash
psql -U postgres -d referee_scheduler -f database/schema.sql
```

3. Set the backend environment variables:

```bash
export JDBC_DATABASE_URL=jdbc:postgresql://localhost:5432/referee_scheduler
export JDBC_DATABASE_USERNAME=postgres
export JDBC_DATABASE_PASSWORD=
export SPRING_JPA_HBM2DDL=validate
```

4. Start the backend:

```bash
cd backend
mvn spring-boot:run
```

Notes:
- `SPRING_JPA_HBM2DDL=validate` expects the schema to already exist.
- Change it to `update` if you want Hibernate to update tables during local development.
- The app reads the database settings from environment variables, so you can override them without editing `application.properties`.

### Frontend Setup
Frontend setup is not wired yet. Add the frontend run steps here once the React app has a package manifest and dev server command.

### Database
The database schema lives in [database/schema.sql](database/schema.sql).

Current tables:
- `users`
- `games`
- `referee_availability`
- `game_assignments`

If you already have the tables created, the backend can connect with the Postgres settings above and start normally.

## License
MIT License - see LICENSE file for details
