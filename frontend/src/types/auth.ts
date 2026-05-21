export type GlobalRole = "ADMIN" | "REFEREE";

export type User = {
    id: number;
    email: string;
    fullName: string;
    phoneNumber: string | null;
    role: GlobalRole;
    createdAt: string;
    updatedAt: string;
};

export type OrganizationMembership = {
    membershipId: number;
    organizationId: number;
    organizationName: string;
    joinCode: string;
    role: GlobalRole;
    createdAt: string;
};

export type AuthResponse = {
    accessToken: string;
    refreshToken: string;
    user: User;
    memberships: OrganizationMembership[];
};

export type JoinCodeValidationResponse = {
    valid: boolean;
    organizationId: number | null;
    organizationName: string | null;
};
