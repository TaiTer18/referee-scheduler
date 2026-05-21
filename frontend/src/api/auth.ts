import { apiFetch } from "./client";
import type { AuthResponse, GlobalRole, JoinCodeValidationResponse } from "../types/auth";

export type RegisterRequest = {
    email: string;
    password: string;
    fullName: string;
    phoneNumber?: string;
    role: GlobalRole;
    organizationName?: string;
    joinCode?: string;
};

export function registerUser(payload: RegisterRequest) {
    return apiFetch<AuthResponse>("/api/auth/register", {
        method: "POST",
        body: JSON.stringify(payload),
    });
}

export function validateJoinCode(joinCode: string) {
    return apiFetch<JoinCodeValidationResponse>(
        `/api/auth/join-code/${encodeURIComponent(joinCode)}`
    );
}
