# Technical Decisions

## Decision: Use a layered Spring Boot structure

### Context
The project needed clear separation between routing, business logic, and persistence.

### Decision
Use controllers, services, repositories, DTOs, and security classes with distinct responsibilities.

### Why
- Easier to reason about
- Easier to test
- Easier to explain in interviews
- Easier to extend when the frontend is added

### Tradeoff
Adds more files up front than a minimal prototype.

## Decision: Use DTOs instead of returning entities directly

### Context
The API needs stable request and response shapes for a future TypeScript frontend.

### Decision
Create separate DTO classes for request and response payloads.

### Why
- Prevents exposing internal entity fields accidentally
- Keeps API contracts explicit
- Makes validation clearer
- Makes frontend integration easier

### Tradeoff
Requires conversion logic between entities and DTOs.

## Decision: Model organization access with a membership table

### Context
The scheduling workflow needed admins to create organizations and referees to join by code. A referee may also belong to more than one organization.

### Decision
Use:
- `User` for global identity
- `Organization` for clubs/leagues
- `OrganizationMembership` for user-to-organization membership

### Why
- Supports referees belonging to multiple organizations
- Avoids duplicating user accounts
- Makes organization boundaries explicit in the data model
- Gives a clean foundation for org-scoped authorization

### Tradeoff
Adds more query and authorization complexity than a single `organizationId` column on `User`.

## Decision: Use JWT authentication with refresh tokens

### Context
The app needed stateless authentication for API requests.

### Decision
Use short-lived JWT access tokens plus refresh tokens for session continuation.

### Why
- Fits API-based frontend/backend architecture well
- Avoids server-side session storage for normal requests
- Works well with a future frontend client

### Tradeoff
More moving parts than basic session authentication.

## Decision: Replace in-memory logout with database-backed refresh tokens

### Context
The first logout approach used an in-memory token blocklist.

### Problem
- Tokens would become valid again after server restart
- Would not scale across multiple backend instances
- Not a strong production design

### Decision
Store refresh tokens hashed in the database and revoke them on logout.

### Why
- Survives server restarts
- Supports proper revocation
- More production-ready

### Tradeoff
Requires refresh token persistence and slightly more auth complexity.

## Decision: Use refresh-token rotation

### Context
If one refresh token is reused forever, a stolen token stays valuable for too long.

### Decision
On every successful refresh:
- revoke the old refresh token
- create a new refresh token
- return a new access token and refresh token pair

### Why
- Reduces replay risk
- Improves overall session security
- Closer to modern production auth design

### Tradeoff
Requires the client to replace stored refresh tokens after every refresh.

## Decision: Use 15-minute access tokens

### Context
After refresh tokens were added, long-lived access tokens no longer made sense.

### Decision
Reduce access token lifetime to 15 minutes.

### Why
- Limits the impact of a stolen access token
- Pairs naturally with refresh tokens
- Better production posture than 24-hour access tokens

### Tradeoff
The frontend must handle token refresh correctly.

## Decision: Use integration tests with MockMvc

### Context
The project is API-heavy and depends on Spring Security, JSON mapping, validation, and persistence all working together.

### Decision
Write integration tests that exercise the application through HTTP-style requests using `MockMvc`.

### Why
- Verifies full request flow
- Catches security and serialization issues
- Provides more confidence than testing services alone

### Tradeoff
These tests are slower and broader than isolated unit tests.

## Decision: Keep entities simple and use integer foreign keys

### Context
The project is still being learned and explained, and readability mattered more than advanced JPA mapping.

### Decision
Use straightforward entity fields like `refereeId`, `assignedRefereeId`, and `userId` instead of immediately modeling every relationship with `@ManyToOne`.

### Why
- Easier to understand while learning
- Easier to explain
- Keeps the first version simple

### Tradeoff
Some richer JPA conveniences are postponed until later.

## Decision: Keep the global user role while introducing membership roles

### Context
The project already used `User.role` for route-level security before organization memberships were added.

### Decision
Keep:
- `User.role` for coarse route access
- `OrganizationMembership.role` for organization-scoped behavior

### Why
- Minimizes refactor risk
- Keeps Spring Security route checks simple
- Still allows the app to understand a user’s role inside each organization

### Tradeoff
Role information now exists in two places, which is less elegant than a pure membership-only role model. This was chosen as a pragmatic transition step.

## Decision: Scope games directly to organizations

### Context
Even if referees can belong to multiple organizations, each game should still belong to exactly one organization.

### Decision
Add `organizationId` directly to `Game`.

### Why
- Makes filtering and authorization much simpler
- Prevents cross-organization scheduling bugs
- Gives a clear ownership boundary for admin actions

### Tradeoff
The game model becomes slightly richer, but the security and query model becomes much easier to reason about.
