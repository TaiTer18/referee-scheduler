# Interview Notes

## Project Summary
I built the backend for a referee scheduling system. The system allows admins to create organizations and games, assign referees, and manage scheduling inside their organizations. Referees can join organizations by code, belong to multiple organizations, view available games, submit availability, and view their assignments.

The backend is built with Spring Boot, PostgreSQL, Spring Security, JWT authentication, refresh-token rotation, and integration tests using MockMvc.

## How I Structured the Backend
I used a layered design:

- Controllers handle HTTP requests
- Services contain the business logic
- Repositories handle database access
- DTOs define API input/output

I chose this because it keeps responsibilities clear and makes the code easier to test and explain.

## Main Backend Features

### Authentication
- User registration and login
- Admin registration creates an organization and join code
- Referees join organizations by join code
- JWT access tokens
- Refresh tokens stored hashed in the database
- Refresh-token rotation
- Logout through refresh-token revocation

### Admin features
- Create, update, list, and delete games
- View referees and their availability
- Assign and unassign referees

### Referee features
- View unassigned games
- Submit availability
- View their availability history
- View assigned games

## How Authentication Works

### Login flow
1. User logs in with email and password
2. Backend verifies credentials through Spring Security
3. Backend returns:
   - short-lived access token
   - refresh token
   - current organization memberships

### Protected request flow
1. Client sends `Authorization: Bearer <access token>`
2. JWT filter validates the token
3. User is loaded into the Spring Security context
4. Controller and service run with authenticated user info

### Organization join flow
1. Admin registers and creates an organization
2. Backend generates a join code
3. Referees register with that join code or join later using `/api/auth/join-organization`
4. Backend creates an organization membership for that user

### Multi-organization referee model
I modeled users and organizations separately:
- `User` is the account identity
- `Organization` is a club or league
- `OrganizationMembership` connects them

That lets a single referee belong to multiple organizations without creating duplicate accounts.

### Refresh flow
1. Client sends refresh token
2. Backend hashes it and looks it up in the database
3. Backend checks that it exists, is not revoked, and is not expired
4. Backend revokes the old refresh token
5. Backend issues a new access token and a new refresh token

### Logout flow
1. Client sends refresh token
2. Backend marks that refresh token revoked
3. Future refresh attempts with it fail

## Why I Changed the Auth Design
I originally had a simpler logout idea using an in-memory blocklist. I replaced it with refresh tokens stored in the database because the blocklist approach would not survive server restarts and would not scale well. The refresh-token rotation approach is much closer to how a production system would work.

I also changed the data model from a single-organization assumption to a membership-based organization model once it became clear that referees should be able to work across multiple organizations.

## Important Technical Decisions I Can Explain

### Why DTOs?
I did not want to return JPA entities directly because DTOs give me cleaner API contracts and prevent accidental exposure of internal fields.

### Why use an organization membership table?
Because a referee may belong to multiple organizations. A join table is the correct way to model that without duplicating user records.

### Why integration tests?
I wanted to verify the real API flow, including routing, JSON serialization, validation, Spring Security, and database behavior.

### Why 15-minute access tokens?
Once refresh tokens were added, a shorter access token lifetime improved security without making the user log in constantly.

### Why hash refresh tokens?
It follows the same security idea as hashing passwords. If the database is exposed, raw refresh tokens should not be immediately usable.

## How I Would Explain the Project to an Interviewer
I would say:

"I built a Spring Boot backend for a referee scheduling application. The backend supports organization-scoped workflows for admins and referees, including organization creation, join-code onboarding, game management, availability tracking, and assignments. I designed it with controllers, services, repositories, and DTOs to keep responsibilities clear. For security, I implemented JWT-based authentication with refresh-token rotation and hashed refresh-token storage in the database. I also modeled referee access with an organization membership table so one referee can belong to multiple organizations. I added integration tests with MockMvc to verify the main auth and scheduling flows end to end."

## Things I Would Improve Next
- Build the TypeScript frontend
- Add Flyway or Liquibase for managed database migrations
- Add more targeted unit tests alongside integration tests
- Add logout-all-sessions support
- Improve refresh-token metadata with device/session tracking
- Add richer organization admin tooling such as invitation management or per-membership roles beyond the current global-role transition model
