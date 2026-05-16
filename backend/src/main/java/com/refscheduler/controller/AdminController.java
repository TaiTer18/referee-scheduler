package com.refscheduler.controller;

import com.refscheduler.dto.admin.AssignmentRequest;
import com.refscheduler.dto.admin.GameRefereeAvailabilityResponse;
import com.refscheduler.dto.admin.RefereeDetailResponse;
import com.refscheduler.dto.common.ApiMessageResponse;
import com.refscheduler.dto.game.GameRequest;
import com.refscheduler.dto.game.GameResponse;
import com.refscheduler.dto.referee.AssignmentResponse;
import com.refscheduler.dto.user.UserResponse;
import com.refscheduler.security.UserPrincipal;
import com.refscheduler.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/games")
    public ResponseEntity<List<GameResponse>> getGames(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(adminService.getAllGames(userPrincipal.getId()));
    }

    @PostMapping("/games")
    public ResponseEntity<GameResponse> createGame(
        @AuthenticationPrincipal UserPrincipal userPrincipal,
        @Valid @RequestBody GameRequest request
    ) {
        return ResponseEntity.ok(adminService.createGame(userPrincipal.getId(), request));
    }

    @PutMapping("/games/{id}")
    public ResponseEntity<GameResponse> updateGame(
        @AuthenticationPrincipal UserPrincipal userPrincipal,
        @PathVariable Integer id,
        @Valid @RequestBody GameRequest request
    ) {
        return ResponseEntity.ok(adminService.updateGame(userPrincipal.getId(), id, request));
    }

    @DeleteMapping("/games/{id}")
    public ResponseEntity<ApiMessageResponse> deleteGame(
        @AuthenticationPrincipal UserPrincipal userPrincipal,
        @PathVariable Integer id
    ) {
        adminService.deleteGame(userPrincipal.getId(), id);
        return ResponseEntity.ok(new ApiMessageResponse("Game deleted successfully."));
    }

    @GetMapping("/games/{id}/referees")
    public ResponseEntity<List<GameRefereeAvailabilityResponse>> getGameReferees(
        @AuthenticationPrincipal UserPrincipal userPrincipal,
        @PathVariable Integer id
    ) {
        return ResponseEntity.ok(adminService.getRefereesForGame(userPrincipal.getId(), id));
    }

    @PostMapping("/assignments")
    public ResponseEntity<AssignmentResponse> assignReferee(
        @AuthenticationPrincipal UserPrincipal userPrincipal,
        @Valid @RequestBody AssignmentRequest request
    ) {
        return ResponseEntity.ok(adminService.assignReferee(userPrincipal.getId(), request));
    }

    @DeleteMapping("/assignments/{id}")
    public ResponseEntity<ApiMessageResponse> unassignReferee(
        @AuthenticationPrincipal UserPrincipal userPrincipal,
        @PathVariable Integer id
    ) {
        adminService.unassignReferee(userPrincipal.getId(), id);
        return ResponseEntity.ok(new ApiMessageResponse("Referee unassigned successfully."));
    }

    @GetMapping("/referees")
    public ResponseEntity<List<UserResponse>> getReferees(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(adminService.getAllReferees(userPrincipal.getId()));
    }

    @GetMapping("/referees/{id}")
    public ResponseEntity<RefereeDetailResponse> getReferee(
        @AuthenticationPrincipal UserPrincipal userPrincipal,
        @PathVariable Integer id
    ) {
        return ResponseEntity.ok(adminService.getRefereeDetails(userPrincipal.getId(), id));
    }
}
