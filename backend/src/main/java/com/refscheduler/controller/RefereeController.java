package com.refscheduler.controller;

import com.refscheduler.dto.game.GameResponse;
import com.refscheduler.dto.referee.AssignmentResponse;
import com.refscheduler.dto.referee.AvailabilityRequest;
import com.refscheduler.dto.referee.AvailabilityStatusResponse;
import com.refscheduler.dto.referee.GameAvailabilityResponse;
import com.refscheduler.security.UserPrincipal;
import com.refscheduler.service.RefereeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/referees")
public class RefereeController {

    private final RefereeService refereeService;

    public RefereeController(RefereeService refereeService) {
        this.refereeService = refereeService;
    }

    @GetMapping("/available-games")
    public ResponseEntity<List<GameResponse>> getAvailableGames() {
        return ResponseEntity.ok(refereeService.getAvailableGames());
    }

    @PostMapping("/availability")
    public ResponseEntity<GameAvailabilityResponse> submitAvailability(
        @AuthenticationPrincipal UserPrincipal userPrincipal,
        @Valid @RequestBody AvailabilityRequest request
    ) {
        if (!"REFEREE".equals(userPrincipal.getRole())) {
            throw new AccessDeniedException("Only referees can submit availability.");
        }
        return ResponseEntity.ok(refereeService.submitAvailability(userPrincipal.getId(), request));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<AvailabilityStatusResponse> getAvailability(@PathVariable Integer id) {
        ensureSelfOrAdmin(id);
        return ResponseEntity.ok(refereeService.getAvailability(id));
    }

    @GetMapping("/{id}/assignments")
    public ResponseEntity<List<AssignmentResponse>> getAssignments(@PathVariable Integer id) {
        ensureSelfOrAdmin(id);
        return ResponseEntity.ok(refereeService.getAssignments(id));
    }

    private void ensureSelfOrAdmin(Integer refereeId) {
        UserPrincipal principal = (UserPrincipal) org.springframework.security.core.context.SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();
        if (!"ADMIN".equals(principal.getRole()) && !principal.getId().equals(refereeId)) {
            throw new AccessDeniedException("You do not have permission to view this referee.");
        }
    }
}
