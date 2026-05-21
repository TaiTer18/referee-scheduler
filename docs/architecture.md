# Architecture

## Overview

The backend follows a typical Spring Boot layered structure:

- Controllers expose HTTP endpoints
- Services contain business logic
- Repositories handle database access
- DTOs define API request and response shapes
- Security components authenticate requests and load the current user
- Organization memberships determine which data a user can access

This separation makes the code easier to test, explain, and extend.

## Package Layout

### Controllers

- `AdminController`
- `AuthController`
- `RefereeController`

Controllers are the HTTP layer. They map routes, accept request bodies, and delegate work to services.

### Services

- `AdminService`
- `AuthService`
- `RefereeService`
- `RefreshTokenService`

Services contain the application rules. This is where the scheduling behavior and authentication flow live.

### Repositories

- `UserRepository`
- `OrganizationRepository`
- `OrganizationMembershipRepository`
- `GameRepository`
- `RefereeAvailabilityRepository`
- `GameAssignmentRepository`
- `RefreshTokenRepository`

Repositories are Spring Data JPA interfaces. They provide CRUD and query methods for database access.

### Models

- `User`
- `Organization`
- `OrganizationMembership`
- `Game`
- `RefereeAvailability`
- `GameAssignment`
- `RefreshToken`

These are the persistence entities mapped to database tables.

### DTOs

DTOs are used so the API contract is separate from the database entities.

Examples:

- `RegisterRequest`
- `LoginRequest`
- `AuthResponse`
- `GameRequest`
- `GameResponse`
- `AssignmentResponse`

### Security

- `SecurityConfig`
- `JwtAuthenticationFilter`
- `JwtService`
- `CustomUserDetailsService`
- `UserPrincipal`

These classes implement authentication and authorization.

## Request Flow

### Example: authenticated request

1. Client sends `Authorization: Bearer <access token>`
2. `JwtAuthenticationFilter` reads and validates the token
3. The filter loads the user and sets authentication in the Spring Security context
4. The controller handles the request
5. The controller calls a service
6. The service uses repositories to read/write data
7. The response DTO is returned as JSON

## Authentication Design

### Access token

- JWT
- Short-lived
- Used on protected API calls

### Refresh token

- Random token string
- Stored hashed in the database
- Rotated on refresh
- Revoked on logout

### Why this design

This is more production-ready than using one long-lived JWT or an in-memory blocklist for logout. It limits damage if an access token is stolen and allows refresh tokens to be revoked safely.

## Organization Model

### User

`User` is the global account identity.

It stores:

- email
- password hash
- name
- phone
- global role

### Organization

`Organization` represents a club, league, or scheduling group.

It stores:

- name
- join code
- created timestamp

### OrganizationMembership

`OrganizationMembership` connects a user to an organization.

It stores:

- `userId`
- `organizationId`
- role within that organization

This allows a referee to belong to many organizations without duplicating user accounts.

### Game ownership

Each `Game` belongs to one organization through `organizationId`.

That is important because:

- admins should only manage games for organizations they belong to
- referees should only see games for organizations they belong to
- assignments and availability should stay inside organization boundaries

## Role Model

### ADMIN

Can:

- create, update, and delete games
- view referees
- assign and unassign referees

Admin actions are allowed only for organizations where the user has an admin membership.

### REFEREE

Can:

- view available games
- submit availability
- view their own availability and assignments

Referees can belong to multiple organizations, so their available games are the union of games from organizations where they have referee memberships.

The backend also enforces:

- self-or-admin checks for referee detail routes
- shared-organization checks for admin access to a referee’s data

## Testing Strategy

The project currently uses integration tests with `MockMvc`.

This means tests cover:

- controllers
- security
- services
- repositories
- JSON request/response flow
- test database behavior

This is especially useful for API-heavy backend work because it verifies the feature works end to end, not just one method at a time.
