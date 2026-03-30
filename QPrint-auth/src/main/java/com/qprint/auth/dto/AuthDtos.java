package com.qprint.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class AuthDtos {
    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 50) String firstName,
            @NotBlank @Size(min = 2, max = 50) String lastName,
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$") String password) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

        public record VerifyEmailRequest(@NotNull UUID userId, @NotBlank @Size(min = 6, max = 6) String code) {}

    public record ResendVerificationRequest(@NotBlank @Email String email) {}

    public record ForgotPasswordRequest(@NotBlank @Email String email) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$") String newPassword,
            @NotBlank String confirmPassword) {}

    public record ProfileUpdateRequest(@NotBlank @Size(min = 2, max = 50) String firstName,
                                       @NotBlank @Size(min = 2, max = 50) String lastName) {}

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$") String newPassword,
                                        @NotBlank String confirmPassword) {}

        public record AuthResponse(UUID userId, String email, String firstName, String lastName, String role, String accessToken, boolean needsVerification) {}
}
