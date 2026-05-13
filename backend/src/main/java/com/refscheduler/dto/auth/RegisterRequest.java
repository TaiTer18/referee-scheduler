package com.refscheduler.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotBlank String fullName,
    String phoneNumber,
    @NotBlank @Pattern(regexp = "REFEREE|ADMIN", message = "Role must be REFEREE or ADMIN") String role
) {
}
