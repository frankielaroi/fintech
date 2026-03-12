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
 * Transaction entity representing a financial transaction in the fintech system.
 * This entity uses BigDecimal for precise monetary calculations and includes
 * comprehensive audit trails, security features, idempotency, and validation
 * required for production fintech applications
 * IMPORTANT:
 * - Never use Double or Float for monetary values in production
 * - All transactions must be immutable once completed
 * - Implement idempotency to prevent duplicate transactions
 */
@Entity
@Table(name = "transactions",
    indexes = {
        @Index(name = "idx_transaction_source_wallet", columnList = "source_wallet_id"),
        @Index(name = "idx_transaction_destination_wallet", columnList = "destination_wallet_id"),
        @Index(name = "idx_transaction_user", columnList = "user_id"),
        @Index(name = "idx_transaction_status", columnList = "status"),
        @Index(name = "idx_transaction_type", columnList = "transaction_type"),
        @Index(name = "idx_transaction_reference", columnList = "transaction_reference", unique = true),
        @Index(name = "idx_transaction_idempotency_key", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_transaction_created_at", columnList = "created_at"),
        @Index(name = "idx_transaction_completed_at", columnList = "completed_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Unique transaction reference number for customer-facing identification.
     * Format: TXN-YYYYMMDD-XXXXXX (e.g., TXN-20260308-ABC123)
     */
    @Column(name = "transaction_reference", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Transaction reference is required")
    private String transactionReference;

    /**
     * Idempotency key to prevent duplicate transactions.
     * Clients should generate a unique key for each transaction request.
     */
    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    /**
     * User who initiated the transaction.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_transaction_user"))
    @NotNull(message = "User is required")
    private User user;

    /**
     * Source wallet (from which money is debited).
     * Can be null for external deposits.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_wallet_id",
        foreignKey = @ForeignKey(name = "fk_transaction_source_wallet"))
    private Wallet sourceWallet;

    /**
     * Destination wallet (to which money is credited).
     * Can be null for external withdrawals.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_wallet_id",
        foreignKey = @ForeignKey(name = "fk_transaction_destination_wallet"))
    private Wallet destinationWallet;

    /**
     * Transaction amount using BigDecimal for precision.
     * Always stored as a positive value; direction determined by transaction type.
     */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    /**
     * Currency code (ISO 4217 format: USD, EUR, GBP, etc.).
     */
    @Column(name = "currency", nullable = false, length = 3)
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code")
    private String currency;

    /**
     * Transaction type defining the nature of the transaction.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    /**
     * Current status of the transaction.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "Transaction status is required")
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    /**
     * Transaction description/memo.
     */
    @Column(name = "description", length = 500)
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * Fee charged for this transaction.
     */
    @Column(name = "fee", precision = 19, scale = 2)
    @DecimalMin(value = "0.00", message = "Fee cannot be negative")
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    /**
     * Tax amount for this transaction.
     */
    @Column(name = "tax", precision = 19, scale = 2)
    @DecimalMin(value = "0.00", message = "Tax cannot be negative")
    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;

    /**
     * Total amount including fees and taxes.
     */
    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Exchange rate used for currency conversion (if applicable).
     */
    @Column(name = "exchange_rate", precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    /**
     * Balance of source wallet before transaction.
     */
    @Column(name = "source_balance_before", precision = 19, scale = 2)
    private BigDecimal sourceBalanceBefore;

    /**
     * Balance of source wallet after transaction.
     */
    @Column(name = "source_balance_after", precision = 19, scale = 2)
    private BigDecimal sourceBalanceAfter;

    /**
     * Balance of destination wallet before transaction.
     */
    @Column(name = "destination_balance_before", precision = 19, scale = 2)
    private BigDecimal destinationBalanceBefore;

    /**
     * Balance of destination wallet after transaction.
     */
    @Column(name = "destination_balance_after", precision = 19, scale = 2)
    private BigDecimal destinationBalanceAfter;

    /**
     * External reference ID (e.g., from payment gateway, bank, etc.).
     */
    @Column(name = "external_reference_id", length = 100)
    private String externalReferenceId;

    /**
     * Payment method used (BANK_TRANSFER, CARD, MOBILE_MONEY, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    /**
     * IP address from which the transaction was initiated.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Device information (browser, mobile app, etc.).
     */
    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    /**
     * Geolocation data for fraud detection.
     */
    @Column(name = "location", length = 255)
    private String location;

    /**
     * Risk score assigned by fraud detection system (0-100).
     */
    @Column(name = "risk_score")
    @Min(value = 0, message = "Risk score must be between 0 and 100")
    @Max(value = 100, message = "Risk score must be between 0 and 100")
    private Integer riskScore;

    /**
     * Flag indicating if the transaction was flagged for review.
     */
    @Column(name = "flagged_for_review", nullable = false)
    @Builder.Default
    private Boolean flaggedForReview = false;

    /**
     * Reason for flagging (fraud, compliance, etc.).
     */
    @Column(name = "flag_reason", length = 500)
    private String flagReason;

    /**
     * Flag indicating if this is a reversal transaction.
     */
    @Column(name = "is_reversal", nullable = false)
    @Builder.Default
    private Boolean isReversal = false;

    /**
     * Reference to the original transaction (if this is a reversal or refund).
     * Using OneToOne because each transaction can only reverse one other transaction.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_transaction_id",
        foreignKey = @ForeignKey(name = "fk_transaction_original"))
    private Transaction originalTransaction;

    /**
     * Reference to the reversal transaction (if this transaction was reversed).
     * Bidirectional OneToOne relationship - this is the inverse side.
     */
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "originalTransaction")
    private Transaction reversalTransaction;

    /**
     * Failure reason if transaction failed.
     */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /**
     * Error code for failed transactions.
     */
    @Column(name = "error_code", length = 50)
    private String errorCode;

    /**
     * Number of retry attempts for this transaction.
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Timestamp when the transaction was initiated.
     */
    @Column(name = "initiated_at")
    private LocalDateTime initiatedAt;

    /**
     * Timestamp when the transaction was completed (success or failure).
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Timestamp when the transaction expires (for pending transactions).
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Settlement date for the transaction (when funds are actually transferred).
     */
    @Column(name = "settlement_date")
    private LocalDateTime settlementDate;

    /**
     * Soft delete flag - never hard delete transaction data for audit purposes.
     */
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    /**
     * Timestamp when the transaction was soft deleted.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * User who deleted this transaction record.
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
     * Enum defining transaction types.
     */
    public enum TransactionType {
        TRANSFER,           // Wallet to wallet transfer
        DEPOSIT,            // External deposit into wallet
        WITHDRAWAL,         // Withdrawal from wallet to external account
        PAYMENT,            // Payment for goods/services
        REFUND,             // Refund of a previous transaction
        FEE,                // Fee deduction
        INTEREST,           // Interest credit
        CASHBACK,           // Cashback reward
        REVERSAL,           // Transaction reversal
        ADJUSTMENT,         // Balance adjustment
        TOPUP,              // Mobile/wallet top-up
        BILL_PAYMENT,       // Utility/bill payment
        PEER_TO_PEER,       // P2P transfer
        MERCHANT_PAYMENT,   // Merchant payment
        ATM_WITHDRAWAL,     // ATM cash withdrawal
        CARD_PAYMENT        // Card payment
    }

    /**
     * Enum defining transaction statuses.
     */
    public enum TransactionStatus {
        PENDING,            // Transaction initiated but not processed
        PROCESSING,         // Transaction is being processed
        COMPLETED,          // Transaction successfully completed
        FAILED,             // Transaction failed
        CANCELLED,          // Transaction cancelled by user
        REVERSED,           // Transaction reversed
        EXPIRED,            // Transaction expired (timeout)
        ON_HOLD,            // Transaction on hold (awaiting approval)
        DECLINED,           // Transaction declined (insufficient funds, etc.)
        REFUNDED            // Transaction refunded
    }

    /**
     * Enum defining payment methods.
     */
    public enum PaymentMethod {
        WALLET,             // Wallet balance
        BANK_TRANSFER,      // Bank transfer
        DEBIT_CARD,         // Debit card
        CREDIT_CARD,        // Credit card
        MOBILE_MONEY,       // Mobile money (M-Pesa, etc.)
        PAYPAL,             // PayPal
        STRIPE,             // Stripe
        CASH,               // Cash
        CHEQUE,             // Cheque
        CRYPTOCURRENCY,     // Cryptocurrency
        UPI,                // UPI (Unified Payments Interface)
        ACH,                // ACH transfer
        WIRE_TRANSFER       // Wire transfer
    }

    /**
     * Check if the transaction is in a final state (cannot be modified).
     */
    public boolean isFinalState() {
        return status == TransactionStatus.COMPLETED
            || status == TransactionStatus.FAILED
            || status == TransactionStatus.CANCELLED
            || status == TransactionStatus.REVERSED
            || status == TransactionStatus.EXPIRED
            || status == TransactionStatus.REFUNDED;
    }

    /**
     * Check if the transaction is successful.
     */
    public boolean isSuccessful() {
        return status == TransactionStatus.COMPLETED;
    }

    /**
     * Check if the transaction can be reversed.
     */
    public boolean canBeReversed() {
        return status == TransactionStatus.COMPLETED
            && !isReversal
            && reversalTransaction == null
            && !deleted;
    }

    /**
     * Check if the transaction can be refunded.
     */
    public boolean canBeRefunded() {
        return status == TransactionStatus.COMPLETED
            && transactionType != TransactionType.REFUND
            && !deleted;
    }

    /**
     * Check if the transaction has expired.
     */
    public boolean hasExpired() {
        return expiresAt != null
            && LocalDateTime.now().isAfter(expiresAt)
            && status == TransactionStatus.PENDING;
    }

    /**
     * Mark the transaction as completed.
     */
    public void markAsCompleted() {
        if (isFinalState()) {
            throw new IllegalStateException("Cannot modify transaction in final state");
        }
        this.status = TransactionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.settlementDate = LocalDateTime.now();
    }

    /**
     * Mark the transaction as failed.
     */
    public void markAsFailed(String reason, String errorCode) {
        if (isFinalState()) {
            throw new IllegalStateException("Cannot modify transaction in final state");
        }
        this.status = TransactionStatus.FAILED;
        this.failureReason = reason;
        this.errorCode = errorCode;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Cancel the transaction.
     */
    public void cancel(String reason) {
        if (isFinalState()) {
            throw new IllegalStateException("Cannot modify transaction in final state");
        }
        if (status != TransactionStatus.PENDING && status != TransactionStatus.ON_HOLD) {
            throw new IllegalStateException("Can only cancel pending or on-hold transactions");
        }
        this.status = TransactionStatus.CANCELLED;
        this.failureReason = reason;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark the transaction for review.
     */
    public void flagForReview(String reason, Integer riskScore) {
        this.flaggedForReview = true;
        this.flagReason = reason;
        this.riskScore = riskScore;
        this.status = TransactionStatus.ON_HOLD;
    }

    /**
     * Increment retry count.
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * Calculate total amount (amount + fee + tax).
     */
    public void calculateTotalAmount() {
        this.totalAmount = this.amount
            .add(this.fee != null ? this.fee : BigDecimal.ZERO)
            .add(this.tax != null ? this.tax : BigDecimal.ZERO);
    }

    /**
     * Mark the transaction as deleted (soft delete).
     */
    public void markAsDeleted(String deletedByUser) {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedByUser;
    }

    /**
     * Validate that transaction has either source or destination wallet.
     */
    public boolean hasValidWallets() {
        return sourceWallet != null || destinationWallet != null;
    }

    /**
     * Pre-persist callback to set initial values.
     */
    @PrePersist
    protected void prePersist() {
        if (initiatedAt == null) {
            initiatedAt = LocalDateTime.now();
        }
        if (totalAmount == null) {
            calculateTotalAmount();
        }
    }
}
