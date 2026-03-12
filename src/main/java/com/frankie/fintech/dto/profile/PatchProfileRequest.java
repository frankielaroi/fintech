package com.frankie.fintech.dto.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Profile patch request. All fields are optional; only provided fields are updated.")
public class PatchProfileRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Schema(description = "Updated full name", example = "Frank Doe")
    @JsonProperty("name")
    private String name;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be in valid E.164 format")
    @Schema(description = "Updated phone number in E.164 format", example = "+15551234567")
    @JsonProperty("phone_number")
    private String phoneNumber;
}

