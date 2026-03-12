package com.frankie.fintech.dto.transaction;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Schema(description = "Request payload for deposit/withdraw wallet operations")
public class DepositWithdrawRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 18, fraction = 2, message = "Amount must have up to 18 integer digits and 2 decimal places")
    @Schema(description = "Transaction amount", example = "100.50", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("amount")
    private BigDecimal amount;

    @NotNull(message = "Source wallet is required")
    @Schema(description = "Source wallet ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("source_wallet")
    @Email
    private String sourceWalletEmail;

    @NotNull(message = "Destination wallet is required")
    @Schema(description = "Destination wallet ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("destination_wallet")
    @Email
    private String destinationWalletEmail;

    @AssertTrue(message = "Source and destination wallets must be different")
    public boolean isWalletCombinationValid() {
        if (sourceWalletEmail == null || destinationWalletEmail == null) {
            return true;
        }
        return !sourceWalletEmail.equals(destinationWalletEmail);
    }
}
