import { create } from "zustand";
import type { AuthResponse, OrganizationMembership, User } from "../types/auth";

type AuthState = {
    accessToken: string | null;
    refreshToken: string | null;
    user: User | null;
    memberships: OrganizationMembership[];
    activeOrganizationId: number | null;
    isAuthenticated: boolean;
    setSession: (payload: AuthResponse) => void;
    clearSession: () => void;
};

function getInitialOrganizationId(memberships: OrganizationMembership[]) {
    return memberships.length > 0 ? memberships[0].organizationId : null;
}

export const useAuthStore = create<AuthState>((set) => ({
    accessToken: null,
    refreshToken: null,
    user: null,
    memberships: [],
    activeOrganizationId: null,
    isAuthenticated: false,

    setSession: (payload) =>
        set({
            accessToken: payload.accessToken,
            refreshToken: payload.refreshToken,
            user: payload.user,
            memberships: payload.memberships,
            activeOrganizationId: getInitialOrganizationId(payload.memberships),
            isAuthenticated: true,
        }),

    clearSession: () =>
        set({
            accessToken: null,
            refreshToken: null,
            user: null,
            memberships: [],
            activeOrganizationId: null,
            isAuthenticated: false,
        }),
}));
