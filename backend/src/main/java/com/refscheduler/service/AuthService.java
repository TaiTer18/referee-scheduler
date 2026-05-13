package com.refscheduler.service;

import com.refscheduler.dto.auth.AuthResponse;
import com.refscheduler.dto.auth.LoginRequest;
import com.refscheduler.dto.auth.RefreshResponse;
import com.refscheduler.dto.auth.RefreshTokenRequest;
import com.refscheduler.dto.auth.RegisterRequest;
import com.refscheduler.dto.user.UserResponse;
import com.refscheduler.exception.ConflictException;
import com.refscheduler.exception.NotFoundException;
import com.refscheduler.exception.UnauthorizedException;
import com.refscheduler.model.RefreshToken;
import com.refscheduler.model.User;
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

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
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

        User user = new User(
            normalizedEmail,
            passwordEncoder.encode(request.password()),
            request.fullName().trim(),
            request.phoneNumber(),
            request.role().trim().toUpperCase()
        );

        User savedUser = userRepository.save(user);
        UserPrincipal principal = new UserPrincipal(savedUser);
        return new AuthResponse(
            jwtService.generateToken(principal),
            refreshTokenService.createRefreshToken(savedUser.getId()),
            UserResponse.from(savedUser)
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
            UserResponse.from(principal.getUser())
        );
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
}
