package com.refscheduler.dto.organization;

import com.refscheduler.model.Organization;
import com.refscheduler.model.OrganizationMembership;

import java.time.LocalDateTime;

public record OrganizationMembershipResponse(
    Integer membershipId,
    Integer organizationId,
    String organizationName,
    String joinCode,
    String role,
    LocalDateTime createdAt
) {
    public static OrganizationMembershipResponse from(OrganizationMembership membership, Organization organization) {
        return new OrganizationMembershipResponse(
            membership.getId(),
            organization.getId(),
            organization.getName(),
            organization.getJoinCode(),
            membership.getRole(),
            membership.getCreatedAt()
        );
    }
}
