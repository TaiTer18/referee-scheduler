package com.refscheduler.service;

import com.refscheduler.dto.admin.AssignmentRequest;
import com.refscheduler.dto.admin.GameRefereeAvailabilityResponse;
import com.refscheduler.dto.admin.RefereeDetailResponse;
import com.refscheduler.dto.game.GameRequest;
import com.refscheduler.dto.game.GameResponse;
import com.refscheduler.dto.referee.AssignmentResponse;
import com.refscheduler.dto.referee.GameAvailabilityResponse;
import com.refscheduler.dto.user.UserResponse;
import com.refscheduler.exception.BadRequestException;
import com.refscheduler.exception.NotFoundException;
import com.refscheduler.model.Game;
import com.refscheduler.model.GameAssignment;
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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final GameRepository gameRepository;
    private final GameAssignmentRepository gameAssignmentRepository;
    private final RefereeAvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;
    private final OrganizationMembershipRepository organizationMembershipRepository;

    public AdminService(
        GameRepository gameRepository,
        GameAssignmentRepository gameAssignmentRepository,
        RefereeAvailabilityRepository availabilityRepository,
        UserRepository userRepository,
        OrganizationMembershipRepository organizationMembershipRepository
    ) {
        this.gameRepository = gameRepository;
        this.gameAssignmentRepository = gameAssignmentRepository;
        this.availabilityRepository = availabilityRepository;
        this.userRepository = userRepository;
        this.organizationMembershipRepository = organizationMembershipRepository;
    }

    public List<GameResponse> getAllGames(Integer adminId) {
        return gameRepository.findByOrganizationIdInOrderByGameDateAscGameTimeAsc(getAdminOrganizationIds(adminId)).stream()
            .map(GameResponse::from)
            .toList();
    }

    @Transactional
    public GameResponse createGame(Integer adminId, GameRequest request) {
        Game game = new Game(
            request.gameDate(),
            request.gameTime(),
            request.location(),
            request.homeTeam(),
            request.awayTeam(),
            request.ageGroup()
        );
        game.setOrganizationId(resolveOrganizationForAdmin(adminId, request.organizationId()));
        game.setNotes(request.notes());
        if (request.status() != null && !request.status().isBlank()) {
            game.setStatus(request.status().trim().toUpperCase());
        }
        return GameResponse.from(gameRepository.save(game));
    }

    @Transactional
    public GameResponse updateGame(Integer adminId, Integer gameId, GameRequest request) {
        Game game = findAccessibleGame(adminId, gameId);
        game.setGameDate(request.gameDate());
        game.setGameTime(request.gameTime());
        game.setLocation(request.location());
        game.setHomeTeam(request.homeTeam());
        game.setAwayTeam(request.awayTeam());
        game.setAgeGroup(request.ageGroup());
        if (request.organizationId() != null && !request.organizationId().equals(game.getOrganizationId())) {
            throw new BadRequestException("Games cannot be moved to a different organization.");
        }
        game.setNotes(request.notes());
        if (request.status() != null && !request.status().isBlank()) {
            game.setStatus(request.status().trim().toUpperCase());
        }
        return GameResponse.from(gameRepository.save(game));
    }

    @Transactional
    public void deleteGame(Integer adminId, Integer gameId) {
        gameRepository.delete(findAccessibleGame(adminId, gameId));
    }

    public List<GameRefereeAvailabilityResponse> getRefereesForGame(Integer adminId, Integer gameId) {
        Game game = findAccessibleGame(adminId, gameId);
        List<User> referees = getRefereesForOrganization(game.getOrganizationId());

        return referees.stream()
            .map(referee -> {
                RefereeAvailability availability = availabilityRepository.findByRefereeIdAndGameId(referee.getId(), gameId)
                    .orElse(null);
                return new GameRefereeAvailabilityResponse(
                    UserResponse.from(referee),
                    availability != null ? availability.getIsAvailable() : null,
                    availability != null ? availability.getUpdatedAt() : null
                );
            })
            .toList();
    }

    @Transactional
    public AssignmentResponse assignReferee(Integer adminId, AssignmentRequest request) {
        User admin = findUser(adminId);
        if (!"ADMIN".equals(admin.getRole())) {
            throw new BadRequestException("Only admins can create assignments.");
        }

        Game game = findAccessibleGame(adminId, request.gameId());
        User referee = findUser(request.refereeId());
        if (!"REFEREE".equals(referee.getRole())) {
            throw new BadRequestException("Assignments must target a referee.");
        }
        if (!organizationMembershipRepository.existsByUserIdAndOrganizationIdAndRole(referee.getId(), game.getOrganizationId(), "REFEREE")) {
            throw new BadRequestException("Referee does not belong to the game's organization.");
        }

        if (game.getAssignedRefereeId() != null) {
            throw new BadRequestException("This game is already assigned.");
        }

        GameAssignment assignment = new GameAssignment(game.getId(), referee.getId(), adminId);
        assignment.setNotes(request.notes());
        GameAssignment savedAssignment = gameAssignmentRepository.save(assignment);

        game.setAssignedRefereeId(referee.getId());
        game.setStatus("ASSIGNED");
        gameRepository.save(game);

        return AssignmentResponse.from(savedAssignment, GameResponse.from(game));
    }

    @Transactional
    public void unassignReferee(Integer adminId, Integer assignmentId) {
        GameAssignment assignment = gameAssignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new NotFoundException("Assignment not found."));

        Game game = findAccessibleGame(adminId, assignment.getGameId());
        assignment.setStatus("CANCELLED");
        gameAssignmentRepository.save(assignment);

        game.setAssignedRefereeId(null);
        game.setStatus("OPEN");
        gameRepository.save(game);
    }

    public List<UserResponse> getAllReferees(Integer adminId) {
        Set<Integer> organizationIds = getAdminOrganizationIds(adminId);
        Set<Integer> refereeIds = organizationMembershipRepository.findByOrganizationIdInAndRole(organizationIds, "REFEREE").stream()
            .map(OrganizationMembership::getUserId)
            .collect(Collectors.toSet());

        return userRepository.findByIdIn(refereeIds).stream()
            .map(UserResponse::from)
            .toList();
    }

    public RefereeDetailResponse getRefereeDetails(Integer adminId, Integer refereeId) {
        User referee = findUser(refereeId);
        if (!"REFEREE".equals(referee.getRole())) {
            throw new BadRequestException("The requested user is not a referee.");
        }
        Set<Integer> sharedOrganizationIds = getSharedOrganizationIds(getAdminOrganizationIds(adminId), getOrganizationIdsForUser(refereeId));
        if (sharedOrganizationIds.isEmpty()) {
            throw new AccessDeniedException("You do not have access to this referee.");
        }

        List<GameAvailabilityResponse> availability = availabilityRepository.findByRefereeId(refereeId)
            .stream()
            .map(entry -> buildAvailabilityIfVisible(entry, sharedOrganizationIds))
            .filter(response -> response != null)
            .toList();

        List<AssignmentResponse> assignments = gameAssignmentRepository.findByRefereeId(refereeId)
            .stream()
            .map(entry -> buildAssignmentIfVisible(entry, sharedOrganizationIds))
            .filter(response -> response != null)
            .toList();

        return new RefereeDetailResponse(UserResponse.from(referee), availability, assignments);
    }

    private User findUser(Integer userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found."));
    }

    private Game findAccessibleGame(Integer adminId, Integer gameId) {
        Game game = findGame(gameId);
        ensureAdminHasOrganizationAccess(adminId, game.getOrganizationId());
        return game;
    }

    private Game findGame(Integer gameId) {
        return gameRepository.findById(gameId)
            .orElseThrow(() -> new NotFoundException("Game not found."));
    }

    private Integer resolveOrganizationForAdmin(Integer adminId, Integer requestedOrganizationId) {
        Set<Integer> adminOrganizationIds = getAdminOrganizationIds(adminId);
        if (requestedOrganizationId != null) {
            if (!adminOrganizationIds.contains(requestedOrganizationId)) {
                throw new AccessDeniedException("You do not belong to that organization.");
            }
            return requestedOrganizationId;
        }
        if (adminOrganizationIds.size() == 1) {
            return adminOrganizationIds.iterator().next();
        }
        throw new BadRequestException("organizationId is required when an admin belongs to multiple organizations.");
    }

    private void ensureAdminHasOrganizationAccess(Integer adminId, Integer organizationId) {
        if (!organizationMembershipRepository.existsByUserIdAndOrganizationIdAndRole(adminId, organizationId, "ADMIN")) {
            throw new AccessDeniedException("You do not have access to this organization.");
        }
    }

    private Set<Integer> getAdminOrganizationIds(Integer adminId) {
        Set<Integer> organizationIds = organizationMembershipRepository.findByUserIdAndRole(adminId, "ADMIN").stream()
            .map(OrganizationMembership::getOrganizationId)
            .collect(Collectors.toSet());
        if (organizationIds.isEmpty()) {
            throw new AccessDeniedException("Admin does not belong to any organizations.");
        }
        return organizationIds;
    }

    private Set<Integer> getOrganizationIdsForUser(Integer userId) {
        return organizationMembershipRepository.findByUserId(userId).stream()
            .map(OrganizationMembership::getOrganizationId)
            .collect(Collectors.toSet());
    }

    private Set<Integer> getSharedOrganizationIds(Collection<Integer> left, Collection<Integer> right) {
        return left.stream()
            .filter(right::contains)
            .collect(Collectors.toSet());
    }

    private List<User> getRefereesForOrganization(Integer organizationId) {
        Set<Integer> refereeIds = organizationMembershipRepository.findByOrganizationIdAndRole(organizationId, "REFEREE").stream()
            .map(OrganizationMembership::getUserId)
            .collect(Collectors.toSet());
        return userRepository.findByIdIn(refereeIds);
    }

    private GameAvailabilityResponse buildAvailabilityIfVisible(RefereeAvailability availability, Set<Integer> visibleOrganizationIds) {
        Game game = findGame(availability.getGameId());
        if (!visibleOrganizationIds.contains(game.getOrganizationId())) {
            return null;
        }
        return GameAvailabilityResponse.from(availability, GameResponse.from(game));
    }

    private AssignmentResponse buildAssignmentIfVisible(GameAssignment assignment, Set<Integer> visibleOrganizationIds) {
        Game game = findGame(assignment.getGameId());
        if (!visibleOrganizationIds.contains(game.getOrganizationId())) {
            return null;
        }
        return AssignmentResponse.from(assignment, GameResponse.from(game));
    }
}
