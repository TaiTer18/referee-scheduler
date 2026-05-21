# RefRoster Frontend Implementation Plan

## 1. Overall Design Philosophy

- **Utility First:** The app is a tool, not a marketing site. Workflows (creating a game, submitting availability) should take as few clicks as possible.
- **Clear Context:** Users (especially those in multiple organizations) must always know _which_ organization's data they are currently viewing.
- **Mobile-First Referee Experience:** Referees will likely use the app on their phones at the field or on the go. The referee dashboard and availability submission _must_ be perfectly responsive. Admins are more likely to use desktops for scheduling.
- **Accessible & High Contrast:** Use standard colors (green for available, red for unavailable, blue for primary actions) and ensure text contrast passes WCAG AA standards.

## 2. Page-by-Page Specifications & Wireframes

### A. Landing Page (Public)

- **Purpose:** Explain the value proposition and drive registration.
- **Layout:**
    - **Header:** Logo (RefRoster) left, Login / Register buttons right.
    - **Hero Section:** Catchy headline ("Simplify Referee Scheduling"), subheadline, and a large primary "Get Started" CTA.
    - **Features Grid:** 3-column layout (mobile: stacked). "For Admins: Effortless scheduling", "For Referees: Manage your time", "Seamless Integration".
    - **Footer:** Simple copyright and links.
- **Required State:** None (Static).

### B. Authentication Pages

- **Login (\`/login\`):**
    - **Layout:** Centered card on a subtle gray background. Logo at top.
    - **Components:** Email input, Password input, "Sign In" button, "Don't have an account? Register" link.
- **Register (\`/register\`):**
    - **Layout:** Centered card, slightly wider.
    - **Components:**
        - Role toggle (Pill shape: [Admin] [Referee]).
        - Common fields: Full Name, Email, Password.
        - Conditional fields: If Admin -> "Organization Name". If Referee -> "Join Code" (Optional).
        - "Create Account" button.
- **Join Organization (\`/app/join\`):**
    - **Layout:** Simple modal or centered card inside the App Shell.
    - **Components:** Large input for 6-digit join code, "Join" button.

### C. App Shell (Protected - \`/app/\*\`)

- **Layout (Desktop):** Left Sidebar (navigation) + Top Header (context) + Main Content Area.
- **Layout (Mobile):** Top Header with Hamburger Menu + Main Content Area + (Optional) Bottom Tab Bar for core actions.
- **Top Header:**
    - Left: Organization Switcher dropdown (displays active organization name).
    - Right: User Avatar/Name dropdown -> Profile, Logout.

### D. Admin Dashboard

- **Home (\`/app/admin\`):**
    - **Widgets:** Total Games (this week), Active Referees, Pending Assignments.
    - **List:** "Upcoming Games Needing Referees".
- **Games Management (\`/app/admin/games\`):**
    - **Header:** Title + "Create New Game" button.
    - **List/Table:** Date, Time, Location, Status (Staffed / Unstaffed). Click row to view details.
- **Game Details (\`/app/admin/games/:id\`):**
    - **Top:** Game info (teams, location, time).
    - **Bottom:** 2 Columns. Left: Assigned Referees. Right: Available Referees (list of refs who submitted availability). Admin clicks a "+" next to an available ref to move them to assigned.
- **Referees Management (\`/app/admin/referees\`):**
    - **Header:** Title + "Invite Referees" (shows join code).
    - **Table:** Name, Email, Games Refereed.

### E. Referee Dashboard

- **Home (\`/app/referee\`):**
    - **Widgets:** Next Assignment (prominent card), Games needing availability.
- **Available Games (\`/app/referee/available-games\`):**
    - **List:** Card layout (better for mobile). Shows Date, Time, Location.
    - **Action:** A clear toggle or button group per game: [Available] [Unavailable].
- **My Assignments (\`/app/referee/assignments\`):**
    - **List:** Chronological list of games the referee is officially assigned to. Includes venue details.

## 3. Component Library Recommendations

Given the 4-6 week timeline, **do not build components from scratch**.

- **Recommendation:** Use **shadcn/ui**.
- **Why shadcn/ui?** It gives you accessible components (Radix UI) but copies the code directly into your project, meaning you can style them with Tailwind exactly how you want.
- **Core components to install:** \`Button\`, \`Input\`, \`Label\`, \`Card\`, \`Dialog\` (Modals), \`DropdownMenu\` (User/Org switcher), \`Table\`, \`Tabs\` (for Role selection on register).

## 4. Color and Typography System

- **Typography:** **Inter** (clean, highly readable, standard for dashboards).
- **Color Palette (Tailwind scale):**
    - **Primary (Brand):** Slate or Zinc (\`bg-slate-900\`, text \`slate-50\`). Professional and modern.
    - **Secondary/Action:** Blue (\`bg-blue-600\` for primary buttons, \`text-blue-600\` for links).
    - **Success (Availability/Assigned):** Emerald (\`bg-emerald-500\`, \`text-emerald-700\`).
    - **Error/Danger (Unavailable/Unassign):** Rose (\`bg-rose-500\`).
    - **Backgrounds:** Page bg: \`bg-slate-50\`. Card bg: \`bg-white\`.

## 5. Responsive Design Strategy (Tailwind)

- **Mobile-First:** Write your base classes for mobile screens.
    - _Example:_ \`flex flex-col md:flex-row\` (Stacked on mobile, side-by-side on desktop).
- **Breakpoints:**
    - \`< 768px (default):\` Mobile view. Sidebar hides behind a hamburger menu. Tables become stacked cards.
    - \`>= 768px (md):\` Tablet view.
    - \`>= 1024px (lg):\` Desktop view. Sidebar is permanently visible on the left.

## 6. Navigation Architecture

- **Public Routing (\`react-router\`):** \`/\`, \`/login\`, \`/register\`
- **Protected Wrapper:** A \`<ProtectedRoute>\` component that checks \`isAuthenticated\` in Zustand. If false, redirect to \`/login\`.
- **Role-Based Routing:**
    - Inside \`<ProtectedRoute>\`, check \`user.role\`.
    - If \`ADMIN\`, render \`<AdminSidebar>\` and allow routes under \`/app/admin/\*\`.
    - If \`REFEREE\`, render \`<RefereeSidebar>\` and allow routes under \`/app/referee/\*\`.

## 7. Form Design Specifications (React Hook Form + Zod)

- **Validation Rules:**
    - \`Email\`: \`z.string().email("Invalid email address")\`
    - \`Password\`: \`z.string().min(8, "Password must be at least 8 characters")\`
    - \`JoinCode\`: \`z.string().length(6, "Join code must be exactly 6 characters")\`
- **UX Best Practices:**
    - Show validation errors immediately below the input field in red (\`text-rose-500 text-sm\`).
    - Disable the submit button and show a loading spinner while the TanStack Query mutation is pending.
    - Clear placeholders (e.g., "Enter 6-digit code").

## 8. Implementation Priorities (4-6 Week Plan)

**Week 1: Scaffolding & Auth (The Foundation)**

1. Initialize Vite + React + TS + Tailwind.
2. Setup React Router, Zustand store (auth), and TanStack Query client.
3. Build Login and Register forms using React Hook Form + Zod.
4. Implement the JWT token handling (save to Zustand, attach to API calls via Axios interceptors).

**Week 2: App Shell & Admin Core**

1. Build the protected layout (Header, Sidebar).
2. Implement the active organization switcher in the header.
3. Build the Admin Games list and "Create Game" form.

**Week 4: The Assignment Loop**

1. Build the Admin "Game Details" view.
2. Show referees who are available for that specific game.
3. Implement the UI to Assign/Unassign a referee from the game.

**Week 5: Polish & Edge Cases**

1. Build the "Join Organization" flow for existing users.
2. Refine mobile responsiveness (test extensively on phone sizes).
3. Add empty states (e.g., "No games scheduled yet" instead of a blank screen).
4. Add toast notifications for success/error actions (e.g., "Game created successfully").

**Week 6: Buffer & Deployment**

1. Fix bugs.
2. Clean up code.
3. Deploy frontend (Vercel, Netlify, or AWS).
