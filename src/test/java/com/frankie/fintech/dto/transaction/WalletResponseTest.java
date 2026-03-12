package com.frankie.fintech.dto.transaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frankie.fintech.entity.Wallet;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void fromMapsWalletIntoApiResponse() {
        UUID walletId = UUID.randomUUID();
        LocalDateTime lastTx = LocalDateTime.of(2026, 3, 11, 10, 15, 30);

        Wallet wallet = Wallet.builder()
            .id(walletId)
            .walletType(Wallet.WalletType.PRIMARY)
            .currency("USD")
            .balance(new BigDecimal("1500.25"))
            .availableBalance(new BigDecimal("1200.25"))
            .heldBalance(new BigDecimal("300.00"))
            .status(Wallet.WalletStatus.ACTIVE)
            .isDefault(true)
            .isFrozen(false)
            .lastTransactionAt(lastTx)
            .build();

        WalletResponse response = WalletResponse.from(wallet);

        assertThat(response.getId()).isEqualTo(walletId);
        assertThat(response.getWalletType()).isEqualTo("PRIMARY");
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getBalance()).isEqualByComparingTo("1500.25");
        assertThat(response.getAvailableBalance()).isEqualByComparingTo("1200.25");
        assertThat(response.getHeldBalance()).isEqualByComparingTo("300.00");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getIsDefault()).isTrue();
        assertThat(response.getIsFrozen()).isFalse();
        assertThat(response.getLastTransactionAt()).isEqualTo(lastTx);
    }

    @Test
    void serializationUsesStableSnakeCaseContract() throws Exception {
        WalletResponse response = WalletResponse.builder()
            .id(UUID.fromString("d290f1ee-6c54-4b01-90e6-d701748f0851"))
            .walletType("PRIMARY")
            .currency("USD")
            .balance(new BigDecimal("100.00"))
            .availableBalance(new BigDecimal("90.00"))
            .heldBalance(new BigDecimal("10.00"))
            .status("ACTIVE")
            .isDefault(true)
            .isFrozen(false)
            .build();

        JsonNode node = objectMapper.readTree(objectMapper.writeValueAsBytes(response));

        assertThat(node.has("wallet_type")).isTrue();
        assertThat(node.has("available_balance")).isTrue();
        assertThat(node.has("held_balance")).isTrue();
        assertThat(node.has("is_default")).isTrue();
        assertThat(node.has("is_frozen")).isTrue();
        assertThat(node.has("walletType")).isFalse();
        assertThat(node.has("availableBalance")).isFalse();
    }

    @Test
    void fromRejectsNullWallet() {
        assertThatThrownBy(() -> WalletResponse.from(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Wallet is required");
    }
}

