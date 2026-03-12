package com.frankie.fintech.dto.register;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Register request containing user sign-up details")
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

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    @Schema(
        description = "User password (minimum 6 characters)",
        example = "password123",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 6
    )
    @JsonProperty("password")
    private String password;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Schema(
        description = "User full name",
        example = "Frank Doe",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 2,
        maxLength = 100
    )
    @JsonProperty("name")
    private String name;
}
