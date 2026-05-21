# RefRoster Deployment Plan

## Goal

Deploy RefRoster so assignors and referees at a local club can use it reliably from their phones and laptops without needing any local setup.

This plan is written for a small real-world rollout:

- a limited number of clubs at first
- real users who need a stable app more than a fancy infrastructure setup
- low maintenance for a solo developer

## Recommended Deployment Shape

Deploy the application as three separate pieces:

1. Frontend
2. Backend API
3. PostgreSQL database

Recommended first production stack:

- Frontend: `Vercel`
- Backend: `Render`
- Database: `Neon Postgres` or `Render Postgres`

Why this stack:

- easy to set up
- HTTPS is handled for you
- works well for a solo developer
- low operational overhead
- simple to explain to real users and interviewers

## High-Level Production Architecture

### Frontend

The React app is the public-facing client.

- serves the landing page
- handles login and registration
- lets referees submit availability
- lets assignors manage games and assignments

The frontend should talk only to the backend API, never directly to the database.

### Backend

The Spring Boot app is the source of truth for business logic.

- authentication
- organization membership
- game management
- availability
- assignments
- authorization rules

The backend should be the only service that connects to PostgreSQL.

### Database

PostgreSQL stores:

- users
- organizations
- organization memberships
- games
- availability
- assignments
- refresh tokens

Use a managed Postgres instance in production. Do not host the database on your laptop or local network for club use.

## Deployment Phases

### Phase 1: Prepare the App for Production

Before deploying, make sure the app is ready to leave local development.

#### Backend

- set a strong `APP_JWT_SECRET`
- set production database environment variables
- set `APP_CORS_ALLOWED_ORIGINS` to the real frontend domain
- confirm `JPA_DDL_AUTO=validate`
- make sure no localhost URLs are hardcoded

#### Frontend

- set the production API base URL
- confirm the app uses HTTPS backend URLs
- verify register and login flows work against a deployed API

#### Database

- make sure the production schema is up to date
- keep a written record of schema changes
- move toward proper migrations with `Flyway`

### Phase 2: Deploy the Database

Deploy the database first.

Recommended first approach:

- create a managed Postgres instance
- create a production database
- create a dedicated application user
- store credentials in the hosting platform, not in source control

After the database is created:

1. run the schema manually using `database/schema.sql`
2. apply any follow-up `ALTER TABLE` statements your local development required
3. verify tables exist before deploying the backend

Example production checks:

```sql
\dt
SELECT * FROM organizations;
SELECT * FROM users;
```

### Phase 3: Deploy the Backend

Deploy the backend second.

Recommended host:

- `Render` web service

Typical backend deployment flow:

1. connect the GitHub repo
2. point the service to the `backend` directory
3. use the Maven build
4. provide all required environment variables
5. deploy and test the health of the API

Likely environment variables:

```env
JDBC_DATABASE_URL=
JDBC_DATABASE_USERNAME=
JDBC_DATABASE_PASSWORD=
APP_JWT_SECRET=
APP_JWT_EXPIRATION_MS=900000
APP_REFRESH_TOKEN_EXPIRATION_DAYS=7
APP_CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com
JPA_DDL_AUTO=validate
SHOW_SQL=false
LOG_LEVEL_APP=INFO
LOG_LEVEL_WEB=INFO
```

After deployment, test these endpoints first:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /api/auth/me`

Use Postman or another API client before bringing the frontend online.

### Phase 4: Deploy the Frontend

Deploy the frontend last.

Recommended host:

- `Vercel`

Typical frontend deployment flow:

1. connect the GitHub repo
2. point the project to the `frontend` directory
3. set the production API URL
4. build and deploy
5. test auth and core workflows in the browser

The frontend should use the deployed backend URL, not `localhost`.

## Recommended Environment Strategy

Use separate environments from the start:

- `local`
- `production`

Later, add:

- `staging`

For now, keeping just local and production is enough.

The important thing is that production secrets never live in the repo.

## Security Checklist

Before club users begin using the app, make sure these are true:

- HTTPS is enabled for frontend and backend
- JWT secret is long and private
- CORS allows only your real frontend origin
- database credentials are stored in deployment settings
- `.env` files are not committed
- production logging does not expose secrets or tokens

## Operational Concerns for a Real Club

This app will be used by real people who are often on mobile and may only notice problems shortly before a game. That means reliability matters more than cleverness.

Key concerns:

- keep downtime low
- keep login predictable
- keep assignment data safe
- keep the UI fast on phones

For the first real rollout, assume users will need:

- password resets later
- clear error messages
- stable session behavior
- confidence that assignments are not disappearing

## Rollout Plan for Your Club

Do not roll this out to everyone at once.

### Step 1: Private Developer Testing

You test:

- registration
- login
- join organization
- admin creates games
- referee submits availability
- admin assigns referee
- referee sees assignment

### Step 2: Small Pilot

Ask 1 assignor and 2 to 4 referees to use it.

During the pilot, watch for:

- confusing screens
- data entry mistakes
- auth issues
- mobile layout problems
- missing workflow steps

### Step 3: Club Rollout

Once the pilot is stable:

- onboard the main assignor
- onboard the rest of the referees
- give a short guide with screenshots
- have one backup way to communicate assignments during the transition

That backup matters. Early production apps always discover something the first real users do that local testing never revealed.

## Recommended Next Improvements Before Broad Rollout

These are the most valuable production improvements to make next:

1. Add `Flyway` for schema migrations
2. Add password reset flow
3. Add email delivery for invites and password resets
4. Add basic monitoring and error logging
5. Add a staging environment
6. Add rate limiting for auth endpoints
7. Add audit-friendly admin activity tracking

## Monitoring and Support

Even a small production app needs some visibility.

At minimum, track:

- failed logins
- registration failures
- backend startup failures
- database connection issues
- unhandled exceptions

Good first tools later:

- host-provided logs
- structured application logs
- Sentry for frontend/backend error monitoring

## Backups and Recovery

For production data, decide this before launch:

- how database backups are handled
- how long backups are kept
- who can access backups
- how to restore the app if a bad deployment happens

Managed Postgres providers usually help here, which is another reason to use one.

## Interview-Friendly Explanation

If you need to explain your deployment approach simply:

> RefRoster is deployed as a three-part web application. The React frontend is hosted separately from the Spring Boot API, and the API connects to a managed PostgreSQL database. I chose a managed hosting approach first so I could focus on reliability, security, and real user workflows instead of server administration.

That is a strong, practical answer.

## First Production Checklist

- database created
- schema applied
- backend deployed
- backend env vars configured
- frontend deployed
- frontend API URL configured
- CORS locked to production frontend domain
- registration tested
- login tested
- refresh tested
- admin game creation tested
- referee availability tested
- assignment flow tested
- mobile experience tested

## Suggested First Hosting Decision

If choosing today, use:

- frontend: `Vercel`
- backend: `Render`
- database: `Neon Postgres`

This is not the only good setup, but it is a strong first production setup for a solo project serving a local club.
