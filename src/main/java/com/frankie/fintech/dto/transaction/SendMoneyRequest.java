package com.frankie.fintech.dto.transaction;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Request payload for wallet-to-wallet send money operations")
public class SendMoneyRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 18, fraction = 2, message = "Amount must have up to 18 integer digits and 2 decimal places")
    @Schema(description = "Transfer amount", example = "50.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("amount")
    private BigDecimal amount;

    @NotNull(message = "Source wallet is required")
    @Schema(description = "Sender wallet ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("source_wallet")
    private UUID sourceWallet;

    @NotNull(message = "Destination wallet is required")
    @Schema(description = "Recipient wallet ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("destination_wallet")
    private UUID destinationWallet;

    @AssertTrue(message = "Source and destination wallets must be different")
    public boolean isWalletCombinationValid() {
        if (sourceWallet == null || destinationWallet == null) {
            return true;
        }
        return !sourceWallet.equals(destinationWallet);
    }
}
