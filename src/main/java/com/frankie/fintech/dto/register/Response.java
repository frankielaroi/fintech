package com.frankie.fintech.dto.register;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Register response indicating the result of a user registration attempt")
public class Response {
    @NotNull
    @Schema(
        description = "Indicates whether the registration was successful",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonProperty("success")
    private Boolean success;

    @Schema(
        description = "Message providing additional information about the registration result",
        example = "User registered successfully"
    )
    @JsonProperty("message")
    private String message;

    public static Response success(String message) {
        return Response.builder()
            .success(true)
            .message(message)
            .build();
    }

    public static Response failure(String message) {
        return Response.builder()
            .success(false)
            .message(message)
            .build();
    }
}
