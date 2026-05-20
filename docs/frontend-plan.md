# Frontend Plan

## Goal

Build a React + TypeScript frontend for the Referee Scheduler that works cleanly with the current Spring Boot backend.

The frontend needs to support:

- admin organization setup and game management
- referee registration and onboarding by join code
- multi-organization referee memberships
- short-lived JWT access tokens
- refresh-token rotation
- role-based and organization-aware workflows

This document is the implementation plan for the frontend.

## Core Product Flows

### Admin first-time flow

1. Admin registers
2. Registration creates an organization
3. Backend returns a join code
4. Admin lands on the admin area
5. Admin creates games
6. Admin shares the join code with referees

### Referee first-time flow

1. Referee registers
2. Referee enters a join code
3. Referee is added to the organization
4. Referee lands on the referee area
5. Referee sees available games
6. Referee submits availability

### Existing referee joins another organization

1. Referee logs in
2. Referee opens a join organization screen
3. Referee enters a new join code
4. Frontend updates membership list
5. Referee can switch organization context in the UI

### Returning user flow

1. User logs in
2. Frontend stores session and memberships
3. User uses the app normally
4. If access token expires, frontend refreshes it automatically
5. User continues without manually logging in again

## Tech Stack

- React
- TypeScript
- Vite
- React Router
- TanStack Query
- Zustand
- React Hook Form
- Zod
- Tailwind CSS

## Why this stack

### Vite

- fast dev server
- minimal setup
- great for React + TypeScript projects

### React Router

- clear page structure
- simple protected route handling

### TanStack Query

- ideal for backend-heavy apps
- handles loading, caching, and invalidation cleanly

### Zustand

- simple global client state
- good fit for auth/session state

### React Hook Form + Zod

- great form DX
- type-safe validation
- useful for register/login/game forms

### Tailwind CSS

- fast iteration
- easy to keep UI consistent
- good for a growing product before a full design system exists

## Frontend Architecture Principles

The frontend should be designed around these ideas:

1. Authentication should feel invisible to the user
2. Organization context should always be clear
3. Admin and referee workflows should be separated in the UI
4. Backend communication should go through one shared API layer
5. Server state and client state should be handled differently

## Important Product Concept: Active Organization

The backend allows referees to belong to multiple organizations.

The frontend should therefore track:

- all memberships
- one active organization id

This is important for:

- filtering visible data in the UI
- reducing confusion for multi-org referees
- future admin multi-org workflows
- making the app feel intentional

Even if the backend already restricts data access, the frontend still needs a clear current organization concept.

## Recommended Folder Structure

```text
frontend/
  src/
    api/
      client.ts
      auth.ts
      admin.ts
      referee.ts
    app/
      App.tsx
      providers.tsx
    components/
      layout/
      navigation/
      ui/
    features/
      auth/
      organizations/
      admin-games/
      admin-referees/
      referee-availability/
      referee-assignments/
    hooks/
    lib/
    pages/
    routes/
    store/
    types/
```

## Why this structure

### `api/`

Contains API functions and shared client logic.

### `app/`

Contains root application wiring such as providers and the main app component.

### `components/`

Contains reusable UI and layout pieces.

### `features/`

Groups code by business area rather than by technical type.

### `store/`

Holds auth/session state.

### `types/`

Defines shared TypeScript models based on backend responses.

## Route Map

### Public routes

- `/login`
- `/register`
- `/join-organization`

### Authenticated app shell

- `/app`
- `/app/profile`
- `/app/organizations`

### Admin routes

- `/app/admin`
- `/app/admin/games`
- `/app/admin/games/new`
- `/app/admin/games/:id`
- `/app/admin/referees`
- `/app/admin/referees/:id`

### Referee routes

- `/app/referee`
- `/app/referee/available-games`
- `/app/referee/availability`
- `/app/referee/assignments`

## Page-by-Page Responsibilities

### `/login`

- email/password form
- call login API
- save session
- redirect user

### `/register`

- allow admin or referee registration
- if admin, show `organizationName`
- if referee, show `joinCode`
- call register API
- save session
- redirect user

### `/join-organization`

- authenticated page
- join code form
- call join organization API
- update membership list in state
- optionally prompt user to switch active organization

### `/app`

- shell layout
- top navigation or sidebar
- active organization display
- role-based navigation links

### `/app/profile`

- display user profile info
- show memberships

### `/app/organizations`

- list memberships
- allow organization switching
- show join codes for admin memberships if desired

### `/app/admin`

- basic admin dashboard
- summary cards or quick links

### `/app/admin/games`

- list games
- filter by active organization
- navigate to create/edit/details

### `/app/admin/games/new`

- create game form
- choose organization if admin belongs to multiple organizations

### `/app/admin/games/:id`

- show game details
- show referee availability
- allow assignment and unassignment

### `/app/admin/referees`

- list accessible referees
- scoped by organizations the admin manages

### `/app/admin/referees/:id`

- show referee details
- show assignments
- show availability

### `/app/referee`

- basic referee dashboard
- summary of assignments and open games

### `/app/referee/available-games`

- list games across memberships
- optionally filter by active organization
- start availability submission flow

### `/app/referee/availability`

- show submitted availability records

### `/app/referee/assignments`

- show assigned games

## State Strategy

Split frontend state into two categories:

### 1. Client state

This is frontend-owned state:

- session
- tokens
- memberships
- active organization

Use Zustand for this.

### 2. Server state

This is backend-owned state:

- games
- assignments
- referee details
- availability
- memberships after refetch

Use TanStack Query for this.

## Auth Store Shape

Recommended auth/session store:

```ts
type AuthState = {
    accessToken: string | null;
    refreshToken: string | null;
    user: User | null;
    memberships: OrganizationMembership[];
    activeOrganizationId: number | null;
    isAuthenticated: boolean;
    setSession: (payload: AuthResponse) => void;
    clearSession: () => void;
    updateTokens: (accessToken: string, refreshToken: string) => void;
    setActiveOrganization: (organizationId: number) => void;
    addMembership: (membership: OrganizationMembership) => void;
};
```

## Active Organization Behavior

When session is created:

- if the user has one membership, set that as active automatically
- if the user has many memberships, default to the first and allow switching

The frontend should display the current organization clearly in the layout.

## API Layer Plan

Create one shared API client and then split business calls into modules.

### `api/client.ts`

Responsibilities:

- set base URL
- attach `Authorization` header
- detect `401`
- call refresh endpoint
- update stored tokens
- retry the original request once
- log out on refresh failure

### `api/auth.ts`

Functions:

- `login`
- `register`
- `logout`
- `refresh`
- `me`
- `joinOrganization`

### `api/admin.ts`

Functions:

- `getGames`
- `createGame`
- `updateGame`
- `deleteGame`
- `getGameReferees`
- `assignReferee`
- `unassignReferee`
- `getReferees`
- `getRefereeDetails`

### `api/referee.ts`

Functions:

- `getAvailableGames`
- `submitAvailability`
- `getAvailability`
- `getAssignments`

## Authentication and Refresh Strategy

The frontend must support short-lived access tokens and refresh-token rotation.

### Desired request behavior

1. Send access token on protected request
2. If backend returns `401`
3. Send refresh token to `/api/auth/refresh`
4. Receive new access token and new refresh token
5. Save both
6. Retry original request one time
7. If refresh fails, clear session and redirect to login

This should happen centrally in the API client, not inside page components.

## TypeScript Models To Define Early

At minimum:

- `User`
- `OrganizationMembership`
- `AuthResponse`
- `RefreshResponse`
- `Game`
- `Assignment`
- `GameAvailability`
- `AvailabilityStatusResponse`
- `RefereeDetailResponse`
- `ApiMessageResponse`

## Suggested Type Shapes

```ts
type User = {
    id: number;
    email: string;
    fullName: string;
    phoneNumber: string | null;
    role: "ADMIN" | "REFEREE";
    createdAt: string;
    updatedAt: string;
};

type OrganizationMembership = {
    membershipId: number;
    organizationId: number;
    organizationName: string;
    joinCode: string;
    role: "ADMIN" | "REFEREE";
    createdAt: string;
};

type AuthResponse = {
    accessToken: string;
    refreshToken: string;
    user: User;
    memberships: OrganizationMembership[];
};

type RefreshResponse = {
    accessToken: string;
    refreshToken: string;
};
```

## UI Rules

### Navigation

Show links based on the user’s global role:

- admins see admin navigation
- referees see referee navigation

### Organization switcher

If a user has multiple memberships:

- show an organization selector in the header or sidebar

### Game creation

If an admin belongs to multiple organizations:

- require choosing which organization the game belongs to

### Referee views

A referee should be able to:

- see available games across memberships
- filter by active organization
- submit availability only where allowed

## MVP Plan

Do not build everything at once.

### Phase 1: foundation

- scaffold frontend
- install libraries
- add routing
- add auth store
- add shared API client
- implement refresh handling

### Phase 2: auth screens

- build login page
- build register page
- persist session
- build protected route guard

### Phase 3: core app shell

- build app layout
- build nav
- show current user
- show active organization selector

### Phase 4: admin games

- build games list
- build create game page
- build game details page

### Phase 5: referee core flow

- build available games page
- build submit availability flow
- build assignments page

### Phase 6: organization features

- build join organization page
- build organizations page
- show memberships

### Phase 7: admin referee management

- build referee list
- build referee details page

## Recommended Implementation Order

1. Create Vite React TypeScript app
2. Install Router, Query, Zustand, forms, Zod, Tailwind
3. Define shared types
4. Build auth store
5. Build API client with refresh retry
6. Add public routes
7. Add protected layout
8. Build admin games list
9. Build referee available games page
10. Add join organization workflow
11. Build remaining pages

## Detailed MVP Checklist

### Foundation

- [ ] Create frontend project
- [ ] Configure Tailwind
- [ ] Configure React Router
- [ ] Configure TanStack Query provider
- [ ] Configure Zustand store

### Auth

- [ ] Login page
- [ ] Register page
- [ ] Auth store session persistence
- [ ] Logout flow
- [ ] Refresh-token handling in API client

### App shell

- [ ] Protected route wrapper
- [ ] Main layout
- [ ] Header or sidebar
- [ ] Organization switcher
- [ ] Profile summary

### Admin

- [ ] Games list page
- [ ] Create game page
- [ ] Game details page
- [ ] Referee availability view
- [ ] Assign referee flow

### Referee

- [ ] Available games page
- [ ] Availability submission
- [ ] Assignments page
- [ ] Availability history page

### Organizations

- [ ] Join organization page
- [ ] Membership list page
- [ ] Active organization switching

## Design Notes

The frontend should not be built as one giant dashboard page.

Keep:

- admin workflows separate from referee workflows
- organization context visible
- API interaction centralized
- forms isolated in feature folders

This will make the project easier to maintain and easier to explain in interviews.

## Future Improvements

After the MVP:

- add a more polished dashboard for each role
- add invitation workflows instead of only shared join codes
- add better empty/loading states
- add optimistic updates where useful
- add frontend tests
- add a design system or reusable component library

## Best Next Step

The best practical next step is:

1. scaffold the frontend app
2. build the auth/session layer first
3. build the admin games list and referee available games page as the first real screens

That will give you a working frontend shell around the backend you already built.
