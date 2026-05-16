package com.refscheduler.repository;

import com.refscheduler.model.OrganizationMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, Integer> {
    List<OrganizationMembership> findByUserId(Integer userId);
    List<OrganizationMembership> findByUserIdAndRole(Integer userId, String role);
    List<OrganizationMembership> findByOrganizationId(Integer organizationId);
    List<OrganizationMembership> findByOrganizationIdAndRole(Integer organizationId, String role);
    List<OrganizationMembership> findByOrganizationIdInAndRole(Collection<Integer> organizationIds, String role);
    Optional<OrganizationMembership> findByUserIdAndOrganizationId(Integer userId, Integer organizationId);
    boolean existsByUserIdAndOrganizationId(Integer userId, Integer organizationId);
    boolean existsByUserIdAndOrganizationIdAndRole(Integer userId, Integer organizationId, String role);
}
