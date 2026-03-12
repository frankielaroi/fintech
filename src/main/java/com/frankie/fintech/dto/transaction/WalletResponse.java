package com.frankie.fintech.dto.transaction;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.frankie.fintech.entity.Wallet;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Wallet response with balances and operational state")
public class WalletResponse {

    @Schema(description = "Wallet identifier", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
    @JsonProperty("id")
    private UUID id;

    @Schema(description = "Wallet type", example = "PRIMARY")
    @JsonProperty("wallet_type")
    private String walletType;

    @Schema(description = "Wallet currency in ISO 4217 format", example = "USD")
    @JsonProperty("currency")
    private String currency;

    @Schema(description = "Current total balance", example = "1500.25")
    @JsonProperty("balance")
    private BigDecimal balance;

    @Schema(description = "Available spendable balance", example = "1200.25")
    @JsonProperty("available_balance")
    private BigDecimal availableBalance;

    @Schema(description = "Funds currently held", example = "300.00")
    @JsonProperty("held_balance")
    private BigDecimal heldBalance;

    @Schema(description = "Lifecycle status", example = "ACTIVE")
    @JsonProperty("status")
    private String status;

    @Schema(description = "Whether this is the user's default wallet", example = "true")
    @JsonProperty("is_default")
    private Boolean isDefault;

    @Schema(description = "Whether wallet operations are currently frozen", example = "false")
    @JsonProperty("is_frozen")
    private Boolean isFrozen;

    @Schema(description = "Timestamp of the latest transaction", example = "2026-03-11T10:15:30")
    @JsonProperty("last_transaction_at")
    private LocalDateTime lastTransactionAt;

    public static WalletResponse from(Wallet wallet) {
        if (wallet == null) {
            throw new IllegalArgumentException("Wallet is required");
        }

        return WalletResponse.builder()
            .id(wallet.getId())
            .walletType(wallet.getWalletType() == null ? null : wallet.getWalletType().name())
            .currency(wallet.getCurrency())
            .balance(wallet.getBalance())
            .availableBalance(wallet.getAvailableBalance())
            .heldBalance(wallet.getHeldBalance())
            .status(wallet.getStatus() == null ? null : wallet.getStatus().name())
            .isDefault(wallet.getIsDefault())
            .isFrozen(wallet.getIsFrozen())
            .lastTransactionAt(wallet.getLastTransactionAt())
            .build();
    }
}
