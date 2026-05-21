package com.refscheduler.service;

import com.refscheduler.dto.auth.AuthResponse;
import com.refscheduler.dto.auth.JoinOrganizationRequest;
import com.refscheduler.dto.auth.LoginRequest;
import com.refscheduler.dto.auth.RefreshResponse;
import com.refscheduler.dto.auth.RefreshTokenRequest;
import com.refscheduler.dto.auth.RegisterRequest;
import com.refscheduler.dto.organization.JoinCodeValidationResponse;
import com.refscheduler.dto.organization.OrganizationMembershipResponse;
import com.refscheduler.dto.user.UserResponse;
import com.refscheduler.exception.BadRequestException;
import com.refscheduler.exception.ConflictException;
import com.refscheduler.exception.NotFoundException;
import com.refscheduler.exception.UnauthorizedException;
import com.refscheduler.model.Organization;
import com.refscheduler.model.OrganizationMembership;
import com.refscheduler.model.RefreshToken;
import com.refscheduler.model.User;
import com.refscheduler.repository.OrganizationMembershipRepository;
import com.refscheduler.repository.OrganizationRepository;
import com.refscheduler.repository.UserRepository;
import com.refscheduler.security.JwtService;
import com.refscheduler.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository organizationMembershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
        UserRepository userRepository,
        OrganizationRepository organizationRepository,
        OrganizationMembershipRepository organizationMembershipRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.organizationMembershipRepository = organizationMembershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ConflictException("An account with that email already exists.");
        }

        String normalizedRole = request.role().trim().toUpperCase(Locale.ROOT);
        User user = new User(
            normalizedEmail,
            passwordEncoder.encode(request.password()),
            request.fullName().trim(),
            request.phoneNumber(),
            normalizedRole
        );

        User savedUser = userRepository.save(user);
        createInitialMembership(savedUser, request, normalizedRole);
        UserPrincipal principal = new UserPrincipal(savedUser);
        return new AuthResponse(
            jwtService.generateToken(principal),
            refreshTokenService.createRefreshToken(savedUser.getId()),
            UserResponse.from(savedUser),
            buildMembershipResponses(savedUser.getId())
        );
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new AuthResponse(
            jwtService.generateToken(principal),
            refreshTokenService.createRefreshToken(principal.getId()),
            UserResponse.from(principal.getUser()),
            buildMembershipResponses(principal.getId())
        );
    }

    @Transactional
    public OrganizationMembershipResponse joinOrganization(Integer userId, JoinOrganizationRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found."));
        Organization organization = organizationRepository.findByJoinCodeAndActiveTrue(request.joinCode().trim().toUpperCase(Locale.ROOT))
            .orElseThrow(() -> new NotFoundException("Active organization not found for that join code."));

        if (organizationMembershipRepository.existsByUserIdAndOrganizationId(user.getId(), organization.getId())) {
            throw new ConflictException("User already belongs to this organization.");
        }

        OrganizationMembership membership = organizationMembershipRepository.save(
            new OrganizationMembership(user.getId(), organization.getId(), user.getRole())
        );

        return OrganizationMembershipResponse.from(membership, organization);
    }

    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revokeToken(request.refreshToken());
    }

    @Transactional
    public RefreshResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.findValidToken(request.refreshToken());
        refreshTokenService.revokeToken(refreshToken);
        User user = userRepository.findById(refreshToken.getUserId())
            .orElseThrow(() -> new NotFoundException("User not found."));
        UserPrincipal principal = new UserPrincipal(user);
        return new RefreshResponse(
            jwtService.generateToken(principal),
            refreshTokenService.createRefreshToken(user.getId())
        );
    }

    public UserResponse me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("User is not authenticated.");
        }
        return UserResponse.from(principal.getUser());
    }

    public JoinCodeValidationResponse validateJoinCode(String joinCode) {
        if (joinCode == null || joinCode.isBlank()) {
            return new JoinCodeValidationResponse(false, null, null);
        }

        return organizationRepository.findByJoinCodeAndActiveTrue(joinCode.trim().toUpperCase(Locale.ROOT))
            .map(organization -> new JoinCodeValidationResponse(true, organization.getId(), organization.getName()))
            .orElseGet(() -> new JoinCodeValidationResponse(false, null, null));
    }

    private void createInitialMembership(User user, RegisterRequest request, String normalizedRole) {
        if ("ADMIN".equals(normalizedRole)) {
            if (request.organizationName() == null || request.organizationName().isBlank()) {
                throw new BadRequestException("organizationName is required for admin registration.");
            }
            Organization organization = organizationRepository.save(
                new Organization(request.organizationName().trim(), generateJoinCode())
            );
            organizationMembershipRepository.save(new OrganizationMembership(user.getId(), organization.getId(), "ADMIN"));
            return;
        }

        if (request.joinCode() == null || request.joinCode().isBlank()) {
            throw new BadRequestException("joinCode is required for referee registration.");
        }
        Organization organization = organizationRepository.findByJoinCodeAndActiveTrue(request.joinCode().trim().toUpperCase(Locale.ROOT))
            .orElseThrow(() -> new NotFoundException("Active organization not found for that join code."));
        organizationMembershipRepository.save(new OrganizationMembership(user.getId(), organization.getId(), "REFEREE"));
    }

    private String generateJoinCode() {
        String joinCode;
        do {
            joinCode = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase(Locale.ROOT);
        } while (organizationRepository.existsByJoinCode(joinCode));
        return joinCode;
    }

    private List<OrganizationMembershipResponse> buildMembershipResponses(Integer userId) {
        return organizationMembershipRepository.findByUserId(userId).stream()
            .map(membership -> {
                Organization organization = organizationRepository.findById(membership.getOrganizationId())
                    .orElseThrow(() -> new NotFoundException("Organization not found."));
                return OrganizationMembershipResponse.from(membership, organization);
            })
            .toList();
    }
}
