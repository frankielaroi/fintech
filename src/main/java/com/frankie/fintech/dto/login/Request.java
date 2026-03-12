package com.frankie.fintech.dto.login;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Login Request DTO
 * Represents the credentials required for user authentication.
 * This DTO is used to validate incoming login requests.
 *
 * @version 1.0
 * @since 2026-03-08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Login request containing user credentials")
public class Request {

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Schema(
        description = "User email address",
        example = "user@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonProperty("email")
    private String email;

    @NotEmpty(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    @Schema(
        description = "User password (minimum 6 characters)",
        example = "password123",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 6
    )
    @JsonProperty("password")
    private String password;
}
