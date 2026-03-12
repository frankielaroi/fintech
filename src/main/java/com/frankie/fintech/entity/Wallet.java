package com.frankie.fintech.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Wallet entity representing a user's financial wallet in the fintech system.
 * This entity uses BigDecimal for precise monetary calculations and includes
 * comprehensive audit trails, security features, and validation required for
 * production fintech applications.
 *
 * IMPORTANT: Never use Double or Float for monetary values in production.
 * Always use BigDecimal to avoid floating-point precision errors.
 */
@Entity
@Table(name = "wallets",
    indexes = {
        @Index(name = "idx_wallet_user_id", columnList = "user_id"),
        @Index(name = "idx_wallet_status", columnList = "status"),
        @Index(name = "idx_wallet_type_currency", columnList = "wallet_type, currency"),
        @Index(name = "idx_wallet_created_at", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_wallet_type_currency",
            columnNames = {"user_id", "wallet_type", "currency"})
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Reference to the user who owns this wallet.
     * Using ManyToOne relationship for proper JPA mapping.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_wallet_user"))
    @NotNull(message = "User is required")
    private User user;

    /**
     * Current balance in the wallet.
     * Using BigDecimal for precise monetary calculations.
     * Scale of 2 for standard currency precision (e.g., $10.50).
     * For cryptocurrencies, you may need higher precision.
     */
    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Balance is required")
    @DecimalMin(value = "0.00", message = "Balance cannot be negative")
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * Available balance (balance minus pending/held amounts).
     * This is what the user can actually spend.
     */
    @Column(name = "available_balance", nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Available balance is required")
    @DecimalMin(value = "0.00", message = "Available balance cannot be negative")
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    /**
     * Amount currently on hold (pending transactions, disputes, etc.).
     */
    @Column(name = "held_balance", nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Held balance is required")
    @DecimalMin(value = "0.00", message = "Held balance cannot be negative")
    @Builder.Default
    private BigDecimal heldBalance = BigDecimal.ZERO;

    /**
     * Currency code (ISO 4217 format: USD, EUR, GBP, etc.).
     */
    @Column(name = "currency", nullable = false, length = 3)
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code")
    @Builder.Default
    private String currency = "USD";

    /**
     * Type of wallet (e.g., PRIMARY, SAVINGS, INVESTMENT, CRYPTO).
     * Allows users to have multiple wallets for different purposes.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type", nullable = false, length = 20)
    @NotNull(message = "Wallet type is required")
    @Builder.Default
    private WalletType walletType = WalletType.PRIMARY;

    /**
     * Wallet status for lifecycle management.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "Wallet status is required")
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;

    /**
     * Flag to indicate if this is the default wallet for the user.
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * Minimum balance threshold for alerts/notifications.
     */
    @Column(name = "min_balance_threshold", precision = 19, scale = 2)
    private BigDecimal minBalanceThreshold;

    /**
     * Maximum balance allowed in this wallet (for regulatory compliance).
     */
    @Column(name = "max_balance_limit", precision = 19, scale = 2)
    private BigDecimal maxBalanceLimit;

    /**
     * Daily spending limit for fraud prevention.
     */
    @Column(name = "daily_limit", precision = 19, scale = 2)
    private BigDecimal dailyLimit;

    /**
     * Monthly spending limit for budget management.
     */
    @Column(name = "monthly_limit", precision = 19, scale = 2)
    private BigDecimal monthlyLimit;

    /**
     * Flag indicating if the wallet is frozen/locked.
     */
    @Column(name = "is_frozen", nullable = false)
    @Builder.Default
    private Boolean isFrozen = false;

    /**
     * Reason for freezing the wallet (security, compliance, user request, etc.).
     */
    @Column(name = "freeze_reason", length = 500)
    private String freezeReason;

    /**
     * Timestamp when the wallet was frozen.
     */
    @Column(name = "frozen_at")
    private LocalDateTime frozenAt;

    /**
     * User/system who froze the wallet.
     */
    @Column(name = "frozen_by", length = 100)
    private String frozenBy;

    /**
     * Last transaction timestamp for activity tracking.
     */
    @Column(name = "last_transaction_at")
    private LocalDateTime lastTransactionAt;

    /**
     * Soft delete flag - never hard delete wallet data for audit purposes.
     */
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    /**
     * Timestamp when the wallet was soft deleted.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * User who deleted this wallet.
     */
    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    /**
     * Audit trail: Record creation timestamp.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Audit trail: Record last update timestamp.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Audit trail: User who created this record.
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    /**
     * Audit trail: User who last modified this record.
     */
    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Version field for optimistic locking to prevent concurrent modification issues.
     * Critical for preventing race conditions in balance updates.
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Additional metadata in JSON format for extensibility.
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Enum defining wallet types.
     */
    public enum WalletType {
        PRIMARY,      // Main spending wallet
        SAVINGS,      // Savings account
        INVESTMENT,   // Investment portfolio
        CRYPTO,       // Cryptocurrency wallet
        ESCROW,       // Escrow/holding wallet
        BUSINESS      // Business account
    }

    /**
     * Enum defining wallet statuses.
     */
    public enum WalletStatus {
        ACTIVE,       // Wallet is active and can be used
        INACTIVE,     // Wallet is inactive
        SUSPENDED,    // Temporarily suspended
        CLOSED        // Permanently closed
    }

    /**
     * Check if the wallet can be used for transactions.
     */
    public boolean isOperational() {
        return status == WalletStatus.ACTIVE
            && !isFrozen
            && !deleted
            && user != null
            && user.isAccountActive();
    }

    /**
     * Check if the wallet has sufficient available balance.
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return isOperational()
            && availableBalance != null
            && availableBalance.compareTo(amount) >= 0;
    }

    /**
     * Check if adding amount would exceed maximum balance limit.
     */
    public boolean wouldExceedMaxLimit(BigDecimal amount) {
        if (maxBalanceLimit == null) {
            return false;
        }
        BigDecimal newBalance = balance.add(amount);
        return newBalance.compareTo(maxBalanceLimit) > 0;
    }

    /**
     * Credit the wallet (add funds).
     * This method updates both balance and available balance.
     */
    public void credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        if (wouldExceedMaxLimit(amount)) {
            throw new IllegalArgumentException("Credit would exceed maximum balance limit");
        }
        this.balance = this.balance.add(amount);
        this.availableBalance = this.availableBalance.add(amount);
        this.lastTransactionAt = LocalDateTime.now();
    }

    /**
     * Debit the wallet (remove funds from available balance).
     */
    public void debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        if (!hasSufficientBalance(amount)) {
            throw new IllegalArgumentException("Insufficient available balance");
        }
        this.balance = this.balance.subtract(amount);
        this.availableBalance = this.availableBalance.subtract(amount);
        this.lastTransactionAt = LocalDateTime.now();
    }

    /**
     * Hold funds (move from available to held).
     */
    public void holdFunds(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Hold amount must be positive");
        }
        if (!hasSufficientBalance(amount)) {
            throw new IllegalArgumentException("Insufficient available balance to hold");
        }
        this.availableBalance = this.availableBalance.subtract(amount);
        this.heldBalance = this.heldBalance.add(amount);
    }

    /**
     * Release held funds back to available balance.
     */
    public void releaseFunds(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Release amount must be positive");
        }
        if (this.heldBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient held balance to release");
        }
        this.heldBalance = this.heldBalance.subtract(amount);
        this.availableBalance = this.availableBalance.add(amount);
    }

    /**
     * Deduct from held balance (when completing a held transaction).
     */
    public void deductHeldFunds(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deduct amount must be positive");
        }
        if (this.heldBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient held balance to deduct");
        }
        this.balance = this.balance.subtract(amount);
        this.heldBalance = this.heldBalance.subtract(amount);
        this.lastTransactionAt = LocalDateTime.now();
    }

    /**
     * Freeze the wallet.
     */
    public void freeze(String reason, String freezeBy) {
        this.isFrozen = true;
        this.freezeReason = reason;
        this.frozenAt = LocalDateTime.now();
        this.frozenBy = freezeBy;
        this.status = WalletStatus.SUSPENDED;
    }

    /**
     * Unfreeze the wallet.
     */
    public void unfreeze() {
        this.isFrozen = false;
        this.freezeReason = null;
        this.frozenAt = null;
        this.frozenBy = null;
        this.status = WalletStatus.ACTIVE;
    }

    /**
     * Mark the wallet as deleted (soft delete).
     */
    public void markAsDeleted(String deletedByUser) {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedByUser;
        this.status = WalletStatus.CLOSED;
    }

    /**
     * Validate balance integrity.
     * Total balance should equal available + held balances.
     */
    public boolean validateBalanceIntegrity() {
        BigDecimal calculatedBalance = availableBalance.add(heldBalance);
        return balance.compareTo(calculatedBalance) == 0;
    }
}
