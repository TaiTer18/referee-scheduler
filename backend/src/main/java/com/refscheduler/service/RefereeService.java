package com.refscheduler.service;

import com.refscheduler.dto.game.GameResponse;
import com.refscheduler.dto.referee.AssignmentResponse;
import com.refscheduler.dto.referee.AvailabilityRequest;
import com.refscheduler.dto.referee.AvailabilityStatusResponse;
import com.refscheduler.dto.referee.GameAvailabilityResponse;
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
public class RefereeService {

    private final GameRepository gameRepository;
    private final RefereeAvailabilityRepository availabilityRepository;
    private final GameAssignmentRepository gameAssignmentRepository;
    private final UserRepository userRepository;

    public RefereeService(
        GameRepository gameRepository,
        RefereeAvailabilityRepository availabilityRepository,
        GameAssignmentRepository gameAssignmentRepository,
        UserRepository userRepository
    ) {
        this.gameRepository = gameRepository;
        this.availabilityRepository = availabilityRepository;
        this.gameAssignmentRepository = gameAssignmentRepository;
        this.userRepository = userRepository;
    }

    public List<GameResponse> getAvailableGames() {
        return gameRepository.findByAssignedRefereeIdIsNullOrderByGameDateAscGameTimeAsc()
            .stream()
            .map(GameResponse::from)
            .toList();
    }

    @Transactional
    public GameAvailabilityResponse submitAvailability(Integer refereeId, AvailabilityRequest request) {
        validateReferee(refereeId);
        Game game = findGame(request.gameId());

        RefereeAvailability availability = availabilityRepository.findByRefereeIdAndGameId(refereeId, request.gameId())
            .orElseGet(() -> new RefereeAvailability(refereeId, request.gameId(), request.isAvailable()));

        availability.setIsAvailable(request.isAvailable());
        RefereeAvailability savedAvailability = availabilityRepository.save(availability);
        return GameAvailabilityResponse.from(savedAvailability, GameResponse.from(game));
    }

    public AvailabilityStatusResponse getAvailability(Integer refereeId) {
        validateReferee(refereeId);
        List<GameAvailabilityResponse> availability = availabilityRepository.findByRefereeId(refereeId)
            .stream()
            .map(entry -> GameAvailabilityResponse.from(entry, GameResponse.from(findGame(entry.getGameId()))))
            .toList();
        return new AvailabilityStatusResponse(refereeId, availability);
    }

    public List<AssignmentResponse> getAssignments(Integer refereeId) {
        validateReferee(refereeId);
        return gameAssignmentRepository.findByRefereeId(refereeId)
            .stream()
            .map(assignment -> AssignmentResponse.from(assignment, GameResponse.from(findGame(assignment.getGameId()))))
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
}
