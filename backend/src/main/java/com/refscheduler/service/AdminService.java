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
import com.refscheduler.model.RefereeAvailability;
import com.refscheduler.model.User;
import com.refscheduler.repository.GameAssignmentRepository;
import com.refscheduler.repository.GameRepository;
import com.refscheduler.repository.RefereeAvailabilityRepository;
import com.refscheduler.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final GameRepository gameRepository;
    private final GameAssignmentRepository gameAssignmentRepository;
    private final RefereeAvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;

    public AdminService(
        GameRepository gameRepository,
        GameAssignmentRepository gameAssignmentRepository,
        RefereeAvailabilityRepository availabilityRepository,
        UserRepository userRepository
    ) {
        this.gameRepository = gameRepository;
        this.gameAssignmentRepository = gameAssignmentRepository;
        this.availabilityRepository = availabilityRepository;
        this.userRepository = userRepository;
    }

    public List<GameResponse> getAllGames() {
        return gameRepository.findAllByOrderByGameDateAscGameTimeAsc().stream()
            .map(GameResponse::from)
            .toList();
    }

    @Transactional
    public GameResponse createGame(GameRequest request) {
        Game game = new Game(
            request.gameDate(),
            request.gameTime(),
            request.location(),
            request.homeTeam(),
            request.awayTeam(),
            request.ageGroup()
        );
        game.setNotes(request.notes());
        if (request.status() != null && !request.status().isBlank()) {
            game.setStatus(request.status().trim().toUpperCase());
        }
        return GameResponse.from(gameRepository.save(game));
    }

    @Transactional
    public GameResponse updateGame(Integer gameId, GameRequest request) {
        Game game = findGame(gameId);
        game.setGameDate(request.gameDate());
        game.setGameTime(request.gameTime());
        game.setLocation(request.location());
        game.setHomeTeam(request.homeTeam());
        game.setAwayTeam(request.awayTeam());
        game.setAgeGroup(request.ageGroup());
        game.setNotes(request.notes());
        if (request.status() != null && !request.status().isBlank()) {
            game.setStatus(request.status().trim().toUpperCase());
        }
        return GameResponse.from(gameRepository.save(game));
    }

    @Transactional
    public void deleteGame(Integer gameId) {
        if (!gameRepository.existsById(gameId)) {
            throw new NotFoundException("Game not found.");
        }
        gameRepository.deleteById(gameId);
    }

    public List<GameRefereeAvailabilityResponse> getRefereesForGame(Integer gameId) {
        findGame(gameId);
        List<User> referees = userRepository.findAll().stream()
            .filter(user -> "REFEREE".equals(user.getRole()))
            .toList();

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

        Game game = findGame(request.gameId());
        User referee = findUser(request.refereeId());
        if (!"REFEREE".equals(referee.getRole())) {
            throw new BadRequestException("Assignments must target a referee.");
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
    public void unassignReferee(Integer assignmentId) {
        GameAssignment assignment = gameAssignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new NotFoundException("Assignment not found."));

        Game game = findGame(assignment.getGameId());
        assignment.setStatus("CANCELLED");
        gameAssignmentRepository.save(assignment);

        game.setAssignedRefereeId(null);
        game.setStatus("OPEN");
        gameRepository.save(game);
    }

    public List<UserResponse> getAllReferees() {
        return userRepository.findAll().stream()
            .filter(user -> "REFEREE".equals(user.getRole()))
            .map(UserResponse::from)
            .toList();
    }

    public RefereeDetailResponse getRefereeDetails(Integer refereeId) {
        User referee = findUser(refereeId);
        if (!"REFEREE".equals(referee.getRole())) {
            throw new BadRequestException("The requested user is not a referee.");
        }

        List<GameAvailabilityResponse> availability = availabilityRepository.findByRefereeId(refereeId)
            .stream()
            .map(entry -> GameAvailabilityResponse.from(entry, GameResponse.from(findGame(entry.getGameId()))))
            .toList();

        List<AssignmentResponse> assignments = gameAssignmentRepository.findByRefereeId(refereeId)
            .stream()
            .map(entry -> AssignmentResponse.from(entry, GameResponse.from(findGame(entry.getGameId()))))
            .toList();

        return new RefereeDetailResponse(UserResponse.from(referee), availability, assignments);
    }

    private User findUser(Integer userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found."));
    }

    private Game findGame(Integer gameId) {
        return gameRepository.findById(gameId)
            .orElseThrow(() -> new NotFoundException("Game not found."));
    }
}
