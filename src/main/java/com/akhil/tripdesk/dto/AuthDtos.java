package com.akhil.tripdesk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank(message = "Name is required") String name,
            @NotBlank @Email(message = "Email must be valid") String email,
            @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String password) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record AuthResponse(String token, String tokenType, Long userId, String name, String role) {}
}
