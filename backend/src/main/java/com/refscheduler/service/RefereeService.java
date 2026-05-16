package com.refscheduler.service;

import com.refscheduler.dto.game.GameResponse;
import com.refscheduler.dto.referee.AssignmentResponse;
import com.refscheduler.dto.referee.AvailabilityRequest;
import com.refscheduler.dto.referee.AvailabilityStatusResponse;
import com.refscheduler.dto.referee.GameAvailabilityResponse;
import com.refscheduler.exception.BadRequestException;
import com.refscheduler.exception.NotFoundException;
import com.refscheduler.model.Game;
import com.refscheduler.model.OrganizationMembership;
import com.refscheduler.model.RefereeAvailability;
import com.refscheduler.model.User;
import com.refscheduler.repository.GameAssignmentRepository;
import com.refscheduler.repository.GameRepository;
import com.refscheduler.repository.OrganizationMembershipRepository;
import com.refscheduler.repository.RefereeAvailabilityRepository;
import com.refscheduler.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RefereeService {

    private final GameRepository gameRepository;
    private final RefereeAvailabilityRepository availabilityRepository;
    private final GameAssignmentRepository gameAssignmentRepository;
    private final UserRepository userRepository;
    private final OrganizationMembershipRepository organizationMembershipRepository;

    public RefereeService(
        GameRepository gameRepository,
        RefereeAvailabilityRepository availabilityRepository,
        GameAssignmentRepository gameAssignmentRepository,
        UserRepository userRepository,
        OrganizationMembershipRepository organizationMembershipRepository
    ) {
        this.gameRepository = gameRepository;
        this.availabilityRepository = availabilityRepository;
        this.gameAssignmentRepository = gameAssignmentRepository;
        this.userRepository = userRepository;
        this.organizationMembershipRepository = organizationMembershipRepository;
    }

    public List<GameResponse> getAvailableGames(Integer refereeId) {
        return gameRepository.findByOrganizationIdInAndAssignedRefereeIdIsNullOrderByGameDateAscGameTimeAsc(
                getOrganizationIdsForUser(refereeId, "REFEREE")
            )
            .stream()
            .map(GameResponse::from)
            .toList();
    }

    @Transactional
    public GameAvailabilityResponse submitAvailability(Integer refereeId, AvailabilityRequest request) {
        validateReferee(refereeId);
        Game game = findGame(request.gameId());
        ensureRefereeHasOrganizationAccess(refereeId, game.getOrganizationId());

        RefereeAvailability availability = availabilityRepository.findByRefereeIdAndGameId(refereeId, request.gameId())
            .orElseGet(() -> new RefereeAvailability(refereeId, request.gameId(), request.isAvailable()));

        availability.setIsAvailable(request.isAvailable());
        RefereeAvailability savedAvailability = availabilityRepository.save(availability);
        return GameAvailabilityResponse.from(savedAvailability, GameResponse.from(game));
    }

    public AvailabilityStatusResponse getAvailability(Integer requesterId, String requesterRole, Integer refereeId) {
        validateReferee(refereeId);
        Set<Integer> visibleOrganizationIds = resolveVisibleOrganizations(requesterId, requesterRole, refereeId);
        List<GameAvailabilityResponse> availability = availabilityRepository.findByRefereeId(refereeId)
            .stream()
            .map(entry -> {
                Game game = findGame(entry.getGameId());
                if (!visibleOrganizationIds.contains(game.getOrganizationId())) {
                    return null;
                }
                return GameAvailabilityResponse.from(entry, GameResponse.from(game));
            })
            .filter(response -> response != null)
            .toList();
        return new AvailabilityStatusResponse(refereeId, availability);
    }

    public List<AssignmentResponse> getAssignments(Integer requesterId, String requesterRole, Integer refereeId) {
        validateReferee(refereeId);
        Set<Integer> visibleOrganizationIds = resolveVisibleOrganizations(requesterId, requesterRole, refereeId);
        return gameAssignmentRepository.findByRefereeId(refereeId)
            .stream()
            .map(assignment -> {
                Game game = findGame(assignment.getGameId());
                if (!visibleOrganizationIds.contains(game.getOrganizationId())) {
                    return null;
                }
                return AssignmentResponse.from(assignment, GameResponse.from(game));
            })
            .filter(response -> response != null)
            .toList();
    }

    private void validateReferee(Integer refereeId) {
        User referee = userRepository.findById(refereeId)
            .orElseThrow(() -> new NotFoundException("Referee not found."));
        if (!"REFEREE".equals(referee.getRole())) {
            throw new BadRequestException("The requested user is not a referee.");
        }
    }

    private Game findGame(Integer gameId) {
        return gameRepository.findById(gameId)
            .orElseThrow(() -> new NotFoundException("Game not found."));
    }

    private void ensureRefereeHasOrganizationAccess(Integer refereeId, Integer organizationId) {
        if (!organizationMembershipRepository.existsByUserIdAndOrganizationIdAndRole(refereeId, organizationId, "REFEREE")) {
            throw new AccessDeniedException("Referee does not belong to this organization.");
        }
    }

    private Set<Integer> resolveVisibleOrganizations(Integer requesterId, String requesterRole, Integer refereeId) {
        Set<Integer> refereeOrganizations = getOrganizationIdsForUser(refereeId, "REFEREE");
        if ("ADMIN".equals(requesterRole)) {
            Set<Integer> adminOrganizations = getOrganizationIdsForUser(requesterId, "ADMIN");
            Set<Integer> sharedOrganizations = adminOrganizations.stream()
                .filter(refereeOrganizations::contains)
                .collect(Collectors.toSet());
            if (sharedOrganizations.isEmpty()) {
                throw new AccessDeniedException("You do not have access to this referee.");
            }
            return sharedOrganizations;
        }
        return refereeOrganizations;
    }

    private Set<Integer> getOrganizationIdsForUser(Integer userId, String role) {
        return organizationMembershipRepository.findByUserIdAndRole(userId, role).stream()
            .map(OrganizationMembership::getOrganizationId)
            .collect(Collectors.toSet());
    }
}
