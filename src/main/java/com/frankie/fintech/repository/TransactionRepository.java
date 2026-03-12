package com.frankie.fintech.repository;

import com.frankie.fintech.entity.Transaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Transaction entity operations.
 * This repository provides production-ready methods for transaction management in a fintech application,
 * including financial operations, security features, soft delete support, and comprehensive audit trail queries.
 *
 * <p>All queries respect soft delete flag unless explicitly stated otherwise.
 * <p>Financial transactions are immutable once completed for audit and compliance purposes.
 *
 * @author Fintech Team
 * @version 1.0
 * @since 2026-03-08
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    // ==================== Basic Lookup Methods ====================

    /**
     * Find a non-deleted transaction by ID.
     *
     * @param id the transaction ID
     * @return Optional containing the transaction if found and not deleted
     */
    @Query("SELECT t FROM Transaction t WHERE t.id = :id AND t.deleted = false")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<Transaction> findById(@Param("id") UUID id);

    /**
     * Find transaction by transaction reference.
     * This is the customer-facing reference number.
     *
     * @param transactionReference the transaction reference
     * @return Optional containing the transaction if found
     */
    @Query("SELECT t FROM Transaction t WHERE t.transactionReference = :transactionReference AND t.deleted = false")
    Optional<Transaction> findByTransactionReference(@Param("transactionReference") String transactionReference);

    /**
     * Find transaction by idempotency key.
     * Critical for preventing duplicate transactions.
     *
     * @param idempotencyKey the idempotency key
     * @return Optional containing the transaction if found
     */
    @Query("SELECT t FROM Transaction t WHERE t.idempotencyKey = :idempotencyKey")
    Optional<Transaction> findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    /**
     * Find transaction by external reference ID.
     * Used for reconciliation with external systems.
     *
     * @param externalReferenceId the external reference ID
     * @return Optional containing the transaction if found
     */
    @Query("SELECT t FROM Transaction t WHERE t.externalReferenceId = :externalReferenceId AND t.deleted = false")
    Optional<Transaction> findByExternalReferenceId(@Param("externalReferenceId") String externalReferenceId);

    /**
     * Find transaction by ID with pessimistic write lock.
     * Use this when updating transaction data to prevent race conditions.
     *
     * @param id the transaction ID
     * @return Optional containing the locked transaction if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.id = :id AND t.deleted = false")
    Optional<Transaction> findByIdWithLock(@Param("id") UUID id);

    // ==================== User & Wallet Queries ====================

    /**
     * Find all transactions for a specific user with pagination.
     *
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return page of user's transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.deleted = false")
    Page<Transaction> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find all transactions for a specific source wallet with pagination.
     *
     * @param walletId the source wallet ID
     * @param pageable pagination parameters
     * @return page of transactions from this wallet
     */
    @Query("SELECT t FROM Transaction t WHERE t.sourceWallet.id = :walletId AND t.deleted = false")
    Page<Transaction> findBySourceWalletId(@Param("walletId") UUID walletId, Pageable pageable);

    /**
     * Find all transactions for a specific destination wallet with pagination.
     *
     * @param walletId the destination wallet ID
     * @param pageable pagination parameters
     * @return page of transactions to this wallet
     */
    @Query("SELECT t FROM Transaction t WHERE t.destinationWallet.id = :walletId AND t.deleted = false")
    Page<Transaction> findByDestinationWalletId(@Param("walletId") UUID walletId, Pageable pageable);

    /**
     * Find all transactions involving a wallet (either source or destination).
     *
     * @param walletId the wallet ID
     * @param pageable pagination parameters
     * @return page of all transactions involving this wallet
     */
    @Query("SELECT t FROM Transaction t WHERE (t.sourceWallet.id = :walletId OR t.destinationWallet.id = :walletId) " +
           "AND t.deleted = false")
    Page<Transaction> findByWalletId(@Param("walletId") UUID walletId, Pageable pageable);

    /**
     * Find transactions between two wallets.
     * Useful for analyzing transaction patterns.
     *
     * @param sourceWalletId the source wallet ID
     * @param destinationWalletId the destination wallet ID
     * @param pageable pagination parameters
     * @return page of transactions between the two wallets
     */
    @Query("SELECT t FROM Transaction t WHERE t.sourceWallet.id = :sourceWalletId " +
           "AND t.destinationWallet.id = :destinationWalletId AND t.deleted = false")
    Page<Transaction> findBySourceAndDestinationWallet(@Param("sourceWalletId") UUID sourceWalletId,
                                                        @Param("destinationWalletId") UUID destinationWalletId,
                                                        Pageable pageable);

    // ==================== Status & Type Queries ====================

    /**
     * Find transactions by status with pagination.
     *
     * @param status the transaction status
     * @param pageable pagination parameters
     * @return page of transactions with the specified status
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.deleted = false")
    Page<Transaction> findByStatus(@Param("status") Transaction.TransactionStatus status, Pageable pageable);

    /**
     * Find transactions by type with pagination.
     *
     * @param transactionType the transaction type
     * @param pageable pagination parameters
     * @return page of transactions with the specified type
     */
    @Query("SELECT t FROM Transaction t WHERE t.transactionType = :transactionType AND t.deleted = false")
    Page<Transaction> findByTransactionType(@Param("transactionType") Transaction.TransactionType transactionType,
                                             Pageable pageable);

    /**
     * Find pending transactions for a user.
     * Important for tracking incomplete transactions.
     *
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return page of pending transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.status = com.frankie.fintech.entity.Transaction.TransactionStatus.PENDING " +
           "AND t.deleted = false")
    Page<Transaction> findPendingTransactionsByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find completed transactions for a user within a date range.
     * Used for transaction history and reporting.
     *
     * @param userId the user ID
     * @param startDate the start date
     * @param endDate the end date
     * @param pageable pagination parameters
     * @return page of completed transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.status = com.frankie.fintech.entity.Transaction.TransactionStatus.COMPLETED " +
           "AND t.completedAt BETWEEN :startDate AND :endDate " +
           "AND t.deleted = false")
    Page<Transaction> findCompletedTransactionsByUserIdAndDateRange(@Param("userId") UUID userId,
                                                                     @Param("startDate") LocalDateTime startDate,
                                                                     @Param("endDate") LocalDateTime endDate,
                                                                     Pageable pageable);

    /**
     * Find failed transactions for analysis.
     *
     * @param pageable pagination parameters
     * @return page of failed transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = com.frankie.fintech.entity.Transaction.TransactionStatus.FAILED " +
           "AND t.deleted = false")
    Page<Transaction> findFailedTransactions(Pageable pageable);

    // ==================== Amount & Currency Queries ====================

    /**
     * Find transactions by amount range.
     * Useful for fraud detection and reporting.
     *
     * @param minAmount minimum amount
     * @param maxAmount maximum amount
     * @param pageable pagination parameters
     * @return page of transactions within the amount range
     */
    @Query("SELECT t FROM Transaction t WHERE t.amount BETWEEN :minAmount AND :maxAmount " +
           "AND t.deleted = false")
    Page<Transaction> findByAmountRange(@Param("minAmount") BigDecimal minAmount,
                                         @Param("maxAmount") BigDecimal maxAmount,
                                         Pageable pageable);

    /**
     * Find transactions above a certain amount threshold.
     * Critical for compliance reporting (e.g., CTR - Currency Transaction Report).
     *
     * @param threshold the amount threshold
     * @param pageable pagination parameters
     * @return page of transactions above threshold
     */
    @Query("SELECT t FROM Transaction t WHERE t.amount >= :threshold AND t.deleted = false")
    Page<Transaction> findTransactionsAboveThreshold(@Param("threshold") BigDecimal threshold, Pageable pageable);

    /**
     * Find transactions by currency.
     *
     * @param currency the currency code
     * @param pageable pagination parameters
     * @return page of transactions in the specified currency
     */
    @Query("SELECT t FROM Transaction t WHERE t.currency = :currency AND t.deleted = false")
    Page<Transaction> findByCurrency(@Param("currency") String currency, Pageable pageable);

    /**
     * Calculate total transaction amount for a user within a date range.
     * Useful for spending analysis and limits.
     *
     * @param userId the user ID
     * @param startDate the start date
     * @param endDate the end date
     * @param status optional status filter
     * @return total amount
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.createdAt BETWEEN :startDate AND :endDate " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND t.deleted = false")
    BigDecimal calculateTotalAmountByUserAndDateRange(@Param("userId") UUID userId,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate,
                                                       @Param("status") Transaction.TransactionStatus status);

    // ==================== Fraud & Security Queries ====================

    /**
     * Find transactions flagged for review.
     * Critical for compliance and fraud prevention.
     *
     * @param pageable pagination parameters
     * @return page of flagged transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.flaggedForReview = true AND t.deleted = false")
    Page<Transaction> findFlaggedTransactions(Pageable pageable);

    /**
     * Find transactions by risk score range.
     *
     * @param minRiskScore minimum risk score
     * @param maxRiskScore maximum risk score
     * @param pageable pagination parameters
     * @return page of transactions within risk score range
     */
    @Query("SELECT t FROM Transaction t WHERE t.riskScore BETWEEN :minRiskScore AND :maxRiskScore " +
           "AND t.deleted = false")
    Page<Transaction> findByRiskScoreRange(@Param("minRiskScore") Integer minRiskScore,
                                            @Param("maxRiskScore") Integer maxRiskScore,
                                            Pageable pageable);

    /**
     * Find high-risk transactions above a threshold.
     *
     * @param riskThreshold the risk score threshold
     * @param pageable pagination parameters
     * @return page of high-risk transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.riskScore >= :riskThreshold AND t.deleted = false " +
           "ORDER BY t.riskScore DESC")
    Page<Transaction> findHighRiskTransactions(@Param("riskThreshold") Integer riskThreshold, Pageable pageable);

    /**
     * Find transactions from a specific IP address.
     * Useful for fraud investigation.
     *
     * @param ipAddress the IP address
     * @param pageable pagination parameters
     * @return page of transactions from the IP
     */
    @Query("SELECT t FROM Transaction t WHERE t.ipAddress = :ipAddress AND t.deleted = false")
    Page<Transaction> findByIpAddress(@Param("ipAddress") String ipAddress, Pageable pageable);

    /**
     * Find multiple transactions from the same user within a short time window.
     * Helps detect rapid-fire transaction attempts (potential fraud).
     *
     * @param userId the user ID
     * @param startTime the start of the time window
     * @param endTime the end of the time window
     * @return list of transactions in the time window
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.createdAt BETWEEN :startTime AND :endTime " +
           "AND t.deleted = false " +
           "ORDER BY t.createdAt")
    List<Transaction> findTransactionsInTimeWindow(@Param("userId") UUID userId,
                                                    @Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);

    // ==================== Reversal & Refund Queries ====================

    /**
     * Find the reversal transaction for a given original transaction.
     *
     * @param originalTransactionId the original transaction ID
     * @return Optional containing the reversal transaction
     */
    @Query("SELECT t FROM Transaction t WHERE t.originalTransaction.id = :originalTransactionId " +
           "AND t.isReversal = true AND t.deleted = false")
    Optional<Transaction> findReversalByOriginalTransactionId(@Param("originalTransactionId") UUID originalTransactionId);

    /**
     * Find all reversal transactions.
     *
     * @param pageable pagination parameters
     * @return page of reversal transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.isReversal = true AND t.deleted = false")
    Page<Transaction> findReversalTransactions(Pageable pageable);

    /**
     * Find refund transactions.
     *
     * @param pageable pagination parameters
     * @return page of refund transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.transactionType = com.frankie.fintech.entity.Transaction.TransactionType.REFUND " +
           "AND t.deleted = false")
    Page<Transaction> findRefundTransactions(Pageable pageable);

    // ==================== Date & Time Queries ====================

    /**
     * Find transactions within a date range.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @param pageable pagination parameters
     * @return page of transactions within the date range
     */
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate " +
           "AND t.deleted = false")
    Page<Transaction> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       Pageable pageable);

    /**
     * Find expired pending transactions.
     * These need to be automatically cancelled.
     *
     * @param currentTime the current time
     * @return list of expired transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = com.frankie.fintech.entity.Transaction.TransactionStatus.PENDING " +
           "AND t.expiresAt < :currentTime AND t.deleted = false")
    List<Transaction> findExpiredPendingTransactions(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Find transactions by settlement date range.
     * Used for accounting and reconciliation.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @param pageable pagination parameters
     * @return page of transactions settled in the date range
     */
    @Query("SELECT t FROM Transaction t WHERE t.settlementDate BETWEEN :startDate AND :endDate " +
           "AND t.deleted = false")
    Page<Transaction> findBySettlementDateRange(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate,
                                                 Pageable pageable);

    // ==================== Update Methods ====================

    /**
     * Update transaction status.
     * Use with caution - transactions should generally be immutable once completed.
     *
     * @param transactionId the transaction ID
     * @param newStatus the new status
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE Transaction t SET t.status = :newStatus, t.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE t.id = :transactionId AND t.deleted = false")
    int updateStatus(@Param("transactionId") UUID transactionId,
                     @Param("newStatus") Transaction.TransactionStatus newStatus);

    /**
     * Mark transaction as completed.
     *
     * @param transactionId the transaction ID
     * @param completedAt the completion timestamp
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE Transaction t SET t.status = com.frankie.fintech.entity.Transaction.TransactionStatus.COMPLETED, " +
           "t.completedAt = :completedAt, t.settlementDate = :completedAt " +
           "WHERE t.id = :transactionId AND t.deleted = false")
    int markAsCompleted(@Param("transactionId") UUID transactionId,
                        @Param("completedAt") LocalDateTime completedAt);

    /**
     * Mark transaction as failed.
     *
     * @param transactionId the transaction ID
     * @param failureReason the reason for failure
     * @param errorCode the error code
     * @param completedAt the completion timestamp
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE Transaction t SET t.status = com.frankie.fintech.entity.Transaction.TransactionStatus.FAILED, " +
           "t.failureReason = :failureReason, t.errorCode = :errorCode, t.completedAt = :completedAt " +
           "WHERE t.id = :transactionId AND t.deleted = false")
    int markAsFailed(@Param("transactionId") UUID transactionId,
                     @Param("failureReason") String failureReason,
                     @Param("errorCode") String errorCode,
                     @Param("completedAt") LocalDateTime completedAt);

    /**
     * Flag transaction for review.
     *
     * @param transactionId the transaction ID
     * @param flagReason the reason for flagging
     * @param riskScore the risk score
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE Transaction t SET t.flaggedForReview = true, t.flagReason = :flagReason, " +
           "t.riskScore = :riskScore, t.status = com.frankie.fintech.entity.Transaction.TransactionStatus.ON_HOLD " +
           "WHERE t.id = :transactionId AND t.deleted = false")
    int flagForReview(@Param("transactionId") UUID transactionId,
                      @Param("flagReason") String flagReason,
                      @Param("riskScore") Integer riskScore);

    /**
     * Increment retry count for a transaction.
     *
     * @param transactionId the transaction ID
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE Transaction t SET t.retryCount = t.retryCount + 1 WHERE t.id = :transactionId")
    int incrementRetryCount(@Param("transactionId") UUID transactionId);

    /**
     * Soft delete a transaction.
     * Financial transactions should never be hard deleted.
     *
     * @param transactionId the transaction ID
     * @param deletedBy the user who deleted the transaction
     * @param deletedAt the deletion timestamp
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE Transaction t SET t.deleted = true, t.deletedAt = :deletedAt, t.deletedBy = :deletedBy " +
           "WHERE t.id = :transactionId")
    int softDeleteTransaction(@Param("transactionId") UUID transactionId,
                              @Param("deletedBy") String deletedBy,
                              @Param("deletedAt") LocalDateTime deletedAt);

    // ==================== Statistics & Reporting Methods ====================

    /**
     * Count transactions by status.
     *
     * @param status the transaction status
     * @return count of transactions
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = :status AND t.deleted = false")
    long countByStatus(@Param("status") Transaction.TransactionStatus status);

    /**
     * Count transactions by type.
     *
     * @param transactionType the transaction type
     * @return count of transactions
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionType = :transactionType AND t.deleted = false")
    long countByTransactionType(@Param("transactionType") Transaction.TransactionType transactionType);

    /**
     * Count transactions for a user within a date range.
     * Useful for rate limiting and fraud detection.
     *
     * @param userId the user ID
     * @param startDate the start date
     * @param endDate the end date
     * @return count of transactions
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.createdAt BETWEEN :startDate AND :endDate " +
           "AND t.deleted = false")
    long countByUserIdAndDateRange(@Param("userId") UUID userId,
                                    @Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Get transaction statistics by status for a date range.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return list of Object arrays containing [status, count, totalAmount]
     */
    @Query("SELECT t.status, COUNT(t), SUM(t.amount) FROM Transaction t " +
           "WHERE t.createdAt BETWEEN :startDate AND :endDate " +
           "AND t.deleted = false " +
           "GROUP BY t.status")
    List<Object[]> getTransactionStatsByStatus(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    /**
     * Get transaction statistics by type for a date range.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return list of Object arrays containing [type, count, totalAmount]
     */
    @Query("SELECT t.transactionType, COUNT(t), SUM(t.amount) FROM Transaction t " +
           "WHERE t.createdAt BETWEEN :startDate AND :endDate " +
           "AND t.deleted = false " +
           "GROUP BY t.transactionType")
    List<Object[]> getTransactionStatsByType(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * Get daily transaction volume for a date range.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return list of Object arrays containing [date, count, totalAmount]
     */
    @Query("SELECT CAST(t.createdAt AS date), COUNT(t), SUM(t.amount) FROM Transaction t " +
           "WHERE t.createdAt BETWEEN :startDate AND :endDate " +
           "AND t.status = com.frankie.fintech.entity.Transaction.TransactionStatus.COMPLETED " +
           "AND t.deleted = false " +
           "GROUP BY CAST(t.createdAt AS date) " +
           "ORDER BY CAST(t.createdAt AS date)")
    List<Object[]> getDailyTransactionVolume(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate total fees collected within a date range.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return total fees
     */
    @Query("SELECT COALESCE(SUM(t.fee), 0) FROM Transaction t " +
           "WHERE t.completedAt BETWEEN :startDate AND :endDate " +
           "AND t.status = com.frankie.fintech.entity.Transaction.TransactionStatus.COMPLETED " +
           "AND t.deleted = false")
    BigDecimal calculateTotalFeesCollected(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Get top users by transaction count.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @param limit the number of top users to return
     * @return list of Object arrays containing [userId, transactionCount]
     */
    @Query("SELECT t.user.id, COUNT(t) FROM Transaction t " +
           "WHERE t.createdAt BETWEEN :startDate AND :endDate " +
           "AND t.deleted = false " +
           "GROUP BY t.user.id " +
           "ORDER BY COUNT(t) DESC " +
           "LIMIT :limit")
    List<Object[]> getTopUsersByTransactionCount(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate,
                                                  @Param("limit") int limit);

    /**
     * Get top users by transaction volume (amount).
     *
     * @param startDate the start date
     * @param endDate the end date
     * @param limit the number of top users to return
     * @return list of Object arrays containing [userId, totalAmount]
     */
    @Query("SELECT t.user.id, SUM(t.amount) FROM Transaction t " +
           "WHERE t.createdAt BETWEEN :startDate AND :endDate " +
           "AND t.status = com.frankie.fintech.entity.Transaction.TransactionStatus.COMPLETED " +
           "AND t.deleted = false " +
           "GROUP BY t.user.id " +
           "ORDER BY SUM(t.amount) DESC " +
           "LIMIT :limit")
    List<Object[]> getTopUsersByTransactionVolume(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate,
                                                   @Param("limit") int limit);

    // ==================== Bulk Operations ====================

    /**
     * Find all transactions by IDs (bulk lookup).
     *
     * @param ids list of transaction IDs
     * @return list of found transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.id IN :ids AND t.deleted = false")
    List<Transaction> findAllByIds(@Param("ids") List<UUID> ids);

    /**
     * Bulk expire pending transactions.
     *
     * @param transactionIds list of transaction IDs
     * @param expiredAt the expiration timestamp
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE Transaction t SET t.status = com.frankie.fintech.entity.Transaction.TransactionStatus.EXPIRED, " +
           "t.completedAt = :expiredAt " +
           "WHERE t.id IN :transactionIds " +
           "AND t.status = com.frankie.fintech.entity.Transaction.TransactionStatus.PENDING " +
           "AND t.deleted = false")
    int bulkExpireTransactions(@Param("transactionIds") List<UUID> transactionIds,
                               @Param("expiredAt") LocalDateTime expiredAt);

    // ==================== Compliance & Audit Methods ====================

    /**
     * Find large transactions above regulatory threshold.
     * Required for CTR (Currency Transaction Report) compliance.
     *
     * @param threshold the reporting threshold (e.g., $10,000)
     * @param startDate the start date
     * @param endDate the end date
     * @param pageable pagination parameters
     * @return page of large transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.amount >= :threshold " +
           "AND t.completedAt BETWEEN :startDate AND :endDate " +
           "AND t.status = com.frankie.fintech.entity.Transaction.TransactionStatus.COMPLETED " +
           "AND t.deleted = false")
    Page<Transaction> findLargeTransactionsForCompliance(@Param("threshold") BigDecimal threshold,
                                                          @Param("startDate") LocalDateTime startDate,
                                                          @Param("endDate") LocalDateTime endDate,
                                                          Pageable pageable);

    /**
     * Find transactions requiring AML (Anti-Money Laundering) review.
     * Includes flagged transactions and those above threshold.
     *
     * @param amountThreshold the amount threshold for review
     * @param pageable pagination parameters
     * @return page of transactions requiring AML review
     */
    @Query("SELECT t FROM Transaction t WHERE (t.flaggedForReview = true OR t.amount >= :amountThreshold) " +
           "AND t.deleted = false")
    Page<Transaction> findTransactionsForAmlReview(@Param("amountThreshold") BigDecimal amountThreshold,
                                                    Pageable pageable);

    /**
     * Generate audit trail for a specific user.
     * Returns all transactions (including deleted) for compliance.
     *
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return page of all user transactions (including deleted)
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId")
    Page<Transaction> findAllTransactionsByUserIdForAudit(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find transactions modified after a certain date.
     * Useful for audit and reconciliation.
     *
     * @param afterDate the date to check
     * @param pageable pagination parameters
     * @return page of modified transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.updatedAt > :afterDate")
    Page<Transaction> findTransactionsModifiedAfter(@Param("afterDate") LocalDateTime afterDate, Pageable pageable);

    // ==================== Custom Projections for Performance ====================

    /**
     * Lightweight transaction projection for listing operations.
     */
    interface TransactionListProjection {
        UUID getId();
        String getTransactionReference();
        BigDecimal getAmount();
        String getCurrency();
        Transaction.TransactionType getTransactionType();
        Transaction.TransactionStatus getStatus();
        LocalDateTime getCreatedAt();
        LocalDateTime getCompletedAt();
    }

    /**
     * Find transactions with lightweight projection.
     *
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return page of transaction projections
     */
    @Query("SELECT t.id as id, t.transactionReference as transactionReference, " +
           "t.amount as amount, t.currency as currency, " +
           "t.transactionType as transactionType, t.status as status, " +
           "t.createdAt as createdAt, t.completedAt as completedAt " +
           "FROM Transaction t WHERE t.user.id = :userId AND t.deleted = false")
    Page<TransactionListProjection> findTransactionsByUserIdLightweight(@Param("userId") UUID userId,
                                                                         Pageable pageable);

    /**
     * Find wallet transactions with lightweight projection.
     *
     * @param walletId the wallet ID
     * @param pageable pagination parameters
     * @return page of transaction projections
     */
    @Query("SELECT t.id as id, t.transactionReference as transactionReference, " +
           "t.amount as amount, t.currency as currency, " +
           "t.transactionType as transactionType, t.status as status, " +
           "t.createdAt as createdAt, t.completedAt as completedAt " +
           "FROM Transaction t WHERE (t.sourceWallet.id = :walletId OR t.destinationWallet.id = :walletId) " +
           "AND t.deleted = false")
    Page<TransactionListProjection> findTransactionsByWalletIdLightweight(@Param("walletId") UUID walletId,
                                                                           Pageable pageable);
}
