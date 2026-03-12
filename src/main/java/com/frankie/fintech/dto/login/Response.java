package com.frankie.fintech.dto.login;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.frankie.fintech.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Login response containing issued tokens and safe user profile data")
public class Response {

    @NotBlank
    @Schema(
        description = "Access token used to authorize API requests",
        example = "eyJhbGciOiJIUzI1NiJ9...",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonProperty("token")
    private String token;

    @NotBlank
    @Schema(
        description = "Refresh token used to obtain a new access token",
        example = "eyJhbGciOiJIUzI1NiJ9.refresh...",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonProperty("refresh_token")
    private String refreshToken;

    @NotNull
    @Schema(
        description = "Minimal user profile returned after successful authentication",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonProperty("user")
    private UserSummary user;

    public static Response from(String token, String refreshToken, User sourceUser) {
        UserSummary summary = UserSummary.builder()
            .id(sourceUser.getId())
            .name(sourceUser.getName())
            .email(sourceUser.getEmail())
            .status(sourceUser.getStatus() != null ? sourceUser.getStatus().name() : null)
            .roles(sourceUser.getRoles() == null
                ? Set.of()
                : sourceUser.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
            .build();

        return Response.builder()
            .token(token)
            .refreshToken(refreshToken)
            .user(summary)
            .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Safe user details for authentication responses")
    public static class UserSummary {

        @NotNull
        @Schema(description = "User identifier", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        @JsonProperty("id")
        private UUID id;

        @NotBlank
        @Schema(description = "User full name", example = "Jane Doe")
        @JsonProperty("name")
        private String name;

        @NotBlank
        @Schema(description = "User email address", example = "jane.doe@example.com")
        @JsonProperty("email")
        private String email;

        @Schema(description = "Current account status", example = "ACTIVE")
        @JsonProperty("status")
        private String status;

        @Schema(description = "Granted role names", example = "[\"ROLE_USER\"]")
        @JsonProperty("roles")
        private Set<String> roles;
    }
}
