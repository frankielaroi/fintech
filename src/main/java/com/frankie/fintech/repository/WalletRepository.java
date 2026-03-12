package com.frankie.fintech.repository;

import com.frankie.fintech.entity.Wallet;
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
 * Repository interface for Wallet entity operations.
 * This repository provides production-ready methods for wallet management in a fintech application,
 * including balance operations, security features, soft delete support, and comprehensive audit trail queries.
 *
 * <p>All queries respect soft delete flag unless explicitly stated otherwise.
 * <p>Balance operations use pessimistic locking to prevent race conditions and ensure data integrity.
 *
 * @author Fintech Team
 * @version 1.0
 * @since 2026-03-08
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    // ==================== Basic Lookup Methods ====================

    /**
     * Find a non-deleted wallet by ID.
     *
     * @param id the wallet ID
     * @return Optional containing the wallet if found and not deleted
     */
    @Query("SELECT w FROM Wallet w WHERE w.id = :id AND w.deleted = false")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<Wallet> findById(@Param("id") UUID id);

    /**
     * Find wallet by ID with pessimistic write lock.
     * CRITICAL: Use this for all balance-modifying operations to prevent race conditions.
     *
     * @param id the wallet ID
     * @return Optional containing the locked wallet if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id AND w.deleted = false")
    Optional<Wallet> findByIdWithLock(@Param("id") UUID id);

    // ==================== User & Wallet Type Queries ====================

    /**
     * Find all active wallets for a user.
     *
     * @param userId the user ID
     * @return list of user's active wallets
     */
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId " +
           "AND w.status = com.frankie.fintech.entity.Wallet.WalletStatus.ACTIVE " +
           "AND w.deleted = false")
    List<Wallet> findActiveWalletsByUserId(@Param("userId") UUID userId);

    /**
     * Find all wallets for a user (including inactive).
     *
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return page of user's wallets
     */
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId AND w.deleted = false")
    Page<Wallet> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find wallet by user ID and wallet type.
     *
     * @param userId the user ID
     * @param walletType the wallet type
     * @return Optional containing the wallet if found
     */
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId " +
           "AND w.walletType = :walletType " +
           "AND w.deleted = false")
    Optional<Wallet> findByUserIdAndWalletType(@Param("userId") UUID userId,
                                                @Param("walletType") Wallet.WalletType walletType);

    /**
     * Find wallet by user ID, wallet type, and currency.
     * Important for multi-currency support.
     *
     * @param userId the user ID
     * @param walletType the wallet type
     * @param currency the currency code
     * @return Optional containing the wallet if found
     */
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId " +
           "AND w.walletType = :walletType " +
           "AND w.currency = :currency " +
           "AND w.deleted = false")
    Optional<Wallet> findByUserIdAndWalletTypeAndCurrency(@Param("userId") UUID userId,
                                                           @Param("walletType") Wallet.WalletType walletType,
                                                           @Param("currency") String currency);

    /**
     * Find wallet by user ID, wallet type, and currency with pessimistic lock.
     * Use this for balance operations.
     *
     * @param userId the user ID
     * @param walletType the wallet type
     * @param currency the currency code
     * @return Optional containing the locked wallet if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId " +
           "AND w.walletType = :walletType " +
           "AND w.currency = :currency " +
           "AND w.deleted = false")
    Optional<Wallet> findByUserIdAndWalletTypeAndCurrencyWithLock(@Param("userId") UUID userId,
                                                                   @Param("walletType") Wallet.WalletType walletType,
                                                                   @Param("currency") String currency);

    /**
     * Find default wallet for a user.
     *
     * @param userId the user ID
     * @return Optional containing the default wallet
     */
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId " +
           "AND w.isDefault = true " +
           "AND w.deleted = false")
    Optional<Wallet> findDefaultWalletByUserId(@Param("userId") UUID userId);

    /**
     * Find default wallet for a user with pessimistic lock.
     *
     * @param userId the user ID
     * @return Optional containing the locked default wallet
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId " +
           "AND w.isDefault = true " +
           "AND w.deleted = false")
    Optional<Wallet> findDefaultWalletByUserIdWithLock(@Param("userId") UUID userId);

    /**
     * Check if a user already has a wallet of a specific type and currency.
     *
     * @param userId the user ID
     * @param walletType the wallet type
     * @param currency the currency code
     * @return true if wallet exists
     */
    @Query("SELECT COUNT(w) > 0 FROM Wallet w WHERE w.user.id = :userId " +
           "AND w.walletType = :walletType " +
           "AND w.currency = :currency " +
           "AND w.deleted = false")
    boolean existsByUserIdAndWalletTypeAndCurrency(@Param("userId") UUID userId,
                                                    @Param("walletType") Wallet.WalletType walletType,
                                                    @Param("currency") String currency);

    /**
     * Count wallets for a user.
     *
     * @param userId the user ID
     * @return count of user's wallets
     */
    @Query("SELECT COUNT(w) FROM Wallet w WHERE w.user.id = :userId AND w.deleted = false")
    long countByUserId(@Param("userId") UUID userId);

    // ==================== Status & Currency Queries ====================

    /**
     * Find wallets by status with pagination.
     *
     * @param status the wallet status
     * @param pageable pagination parameters
     * @return page of wallets with the specified status
     */
    @Query("SELECT w FROM Wallet w WHERE w.status = :status AND w.deleted = false")
    Page<Wallet> findByStatus(@Param("status") Wallet.WalletStatus status, Pageable pageable);

    /**
     * Find wallets by wallet type with pagination.
     *
     * @param walletType the wallet type
     * @param pageable pagination parameters
     * @return page of wallets with the specified type
     */
    @Query("SELECT w FROM Wallet w WHERE w.walletType = :walletType AND w.deleted = false")
    Page<Wallet> findByWalletType(@Param("walletType") Wallet.WalletType walletType, Pageable pageable);

    /**
     * Find wallets by currency with pagination.
     *
     * @param currency the currency code
     * @param pageable pagination parameters
     * @return page of wallets in the specified currency
     */
    @Query("SELECT w FROM Wallet w WHERE w.currency = :currency AND w.deleted = false")
    Page<Wallet> findByCurrency(@Param("currency") String currency, Pageable pageable);

    /**
     * Find frozen wallets.
     *
     * @param pageable pagination parameters
     * @return page of frozen wallets
     */
    @Query("SELECT w FROM Wallet w WHERE w.isFrozen = true AND w.deleted = false")
    Page<Wallet> findFrozenWallets(Pageable pageable);

    /**
     * Find frozen wallets for a specific user.
     *
     * @param userId the user ID
     * @return list of user's frozen wallets
     */
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId " +
           "AND w.isFrozen = true " +
           "AND w.deleted = false")
    List<Wallet> findFrozenWalletsByUserId(@Param("userId") UUID userId);

    // ==================== Balance Queries ====================

    /**
     * Find wallets with balance above a threshold.
     * Useful for high-value account monitoring.
     *
     * @param threshold the balance threshold
     * @param pageable pagination parameters
     * @return page of wallets with balance above threshold
     */
    @Query("SELECT w FROM Wallet w WHERE w.balance >= :threshold AND w.deleted = false")
    Page<Wallet> findWalletsWithBalanceAbove(@Param("threshold") BigDecimal threshold, Pageable pageable);

    /**
     * Find wallets with balance below minimum threshold.
     * Useful for low balance alerts.
     *
     * @param pageable pagination parameters
     * @return page of wallets below their minimum threshold
     */
    @Query("SELECT w FROM Wallet w WHERE w.minBalanceThreshold IS NOT NULL " +
           "AND w.balance < w.minBalanceThreshold " +
           "AND w.deleted = false")
    Page<Wallet> findWalletsBelowMinThreshold(Pageable pageable);

    /**
     * Find wallets near maximum balance limit.
     * For regulatory compliance and user notifications.
     *
     * @param percentage percentage of max limit (e.g., 0.9 for 90%)
     * @param pageable pagination parameters
     * @return page of wallets near their max limit
     */
    @Query("SELECT w FROM Wallet w WHERE w.maxBalanceLimit IS NOT NULL " +
           "AND w.balance >= (w.maxBalanceLimit * :percentage) " +
           "AND w.deleted = false")
    Page<Wallet> findWalletsNearMaxLimit(@Param("percentage") BigDecimal percentage, Pageable pageable);

    /**
     * Find wallets with held balance.
     *
     * @param pageable pagination parameters
     * @return page of wallets with held funds
     */
    @Query("SELECT w FROM Wallet w WHERE w.heldBalance > 0 AND w.deleted = false")
    Page<Wallet> findWalletsWithHeldBalance(Pageable pageable);

    /**
     * Calculate total balance across all wallets for a user.
     *
     * @param userId the user ID
     * @param currency optional currency filter
     * @return total balance
     */
    @Query("SELECT COALESCE(SUM(w.balance), 0) FROM Wallet w WHERE w.user.id = :userId " +
           "AND (:currency IS NULL OR w.currency = :currency) " +
           "AND w.deleted = false")
    BigDecimal calculateTotalBalanceByUserId(@Param("userId") UUID userId,
                                              @Param("currency") String currency);

    /**
     * Calculate total available balance for a user.
     *
     * @param userId the user ID
     * @param currency optional currency filter
     * @return total available balance
     */
    @Query("SELECT COALESCE(SUM(w.availableBalance), 0) FROM Wallet w WHERE w.user.id = :userId " +
           "AND (:currency IS NULL OR w.currency = :currency) " +
           "AND w.deleted = false")
    BigDecimal calculateTotalAvailableBalanceByUserId(@Param("userId") UUID userId,
                                                       @Param("currency") String currency);

    // ==================== Update Methods ====================

    /**
     * Update wallet balance (use with extreme caution).
     * Prefer using the entity methods with pessimistic locking.
     *
     * @param walletId the wallet ID
     * @param newBalance the new balance
     * @param newAvailableBalance the new available balance
     * @param newHeldBalance the new held balance
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE Wallet w SET w.balance = :newBalance, " +
           "w.availableBalance = :newAvailableBalance, " +
           "w.heldBalance = :newHeldBalance, " +
           "w.lastTransactionAt = CURRENT_TIMESTAMP " +
           "WHERE w.id = :walletId AND w.deleted = false")
    int updateBalance(@Param("walletId") UUID walletId,
                      @Param("newBalance") BigDecimal newBalance,
                      @Param("newAvailableBalance") BigDecimal newAvailableBalance,
                      @Param("newHeldBalance") BigDecimal newHeldBalance);

    /**
     * Update last transaction timestamp.
     *
     * @param walletId the wallet ID
     * @param timestamp the transaction timestamp
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE Wallet w SET w.lastTransactionAt = :timestamp WHERE w.id = :walletId")
    int updateLastTransactionAt(@Param("walletId") UUID walletId,
                                 @Param("timestamp") LocalDateTime timestamp);

    /**
     * Set wallet as default for a user.
     *
     * @param walletId the wallet ID to set as default
     * @param userId the user ID
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE Wallet w SET w.isDefault = CASE WHEN w.id = :walletId THEN true ELSE false END " +
           "WHERE w.user.id = :userId AND w.deleted = false")
    int setDefaultWallet(@Param("walletId") UUID walletId, @Param("userId") UUID userId);

    /**
     * Freeze a wallet.
     *
     * @param walletId the wallet ID
     * @param reason the reason for freezing
     * @param frozenBy who froze the wallet
     * @param frozenAt the freeze timestamp
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE Wallet w SET w.isFrozen = true, w.freezeReason = :reason, " +
           "w.frozenBy = :frozenBy, w.frozenAt = :frozenAt, " +
           "w.status = com.frankie.fintech.entity.Wallet.WalletStatus.SUSPENDED " +
           "WHERE w.id = :walletId AND w.deleted = false")
    int freezeWallet(@Param("walletId") UUID walletId,
                     @Param("reason") String reason,
                     @Param("frozenBy") String frozenBy,
                     @Param("frozenAt") LocalDateTime frozenAt);

    /**
     * Unfreeze a wallet.
     *
     * @param walletId the wallet ID
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE Wallet w SET w.isFrozen = false, w.freezeReason = NULL, " +
           "w.frozenBy = NULL, w.frozenAt = NULL, " +
           "w.status = com.frankie.fintech.entity.Wallet.WalletStatus.ACTIVE " +
           "WHERE w.id = :walletId AND w.deleted = false")
    int unfreezeWallet(@Param("walletId") UUID walletId);

    /**
     * Update wallet status.
     *
     * @param walletId the wallet ID
     * @param status the new status
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE Wallet w SET w.status = :status WHERE w.id = :walletId AND w.deleted = false")
    int updateStatus(@Param("walletId") UUID walletId, @Param("status") Wallet.WalletStatus status);

    /**
     * Soft delete a wallet.
     *
     * @param walletId the wallet ID
     * @param deletedBy who deleted the wallet
     * @param deletedAt the deletion timestamp
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE Wallet w SET w.deleted = true, w.deletedAt = :deletedAt, " +
           "w.deletedBy = :deletedBy, w.status = com.frankie.fintech.entity.Wallet.WalletStatus.CLOSED " +
           "WHERE w.id = :walletId")
    int softDeleteWallet(@Param("walletId") UUID walletId,
                         @Param("deletedBy") String deletedBy,
                         @Param("deletedAt") LocalDateTime deletedAt);

    // ==================== Activity & Monitoring Queries ====================

    /**
     * Find inactive wallets (no transactions for a specified period).
     *
     * @param cutoffDate the date threshold for inactivity
     * @param pageable pagination parameters
     * @return page of inactive wallets
     */
    @Query("SELECT w FROM Wallet w WHERE (w.lastTransactionAt IS NULL OR w.lastTransactionAt < :cutoffDate) " +
           "AND w.status = com.frankie.fintech.entity.Wallet.WalletStatus.ACTIVE " +
           "AND w.deleted = false")
    Page<Wallet> findInactiveWallets(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);

    /**
     * Find wallets with recent activity.
     *
     * @param sinceDate the date to check from
     * @param pageable pagination parameters
     * @return page of recently active wallets
     */
    @Query("SELECT w FROM Wallet w WHERE w.lastTransactionAt >= :sinceDate " +
           "AND w.deleted = false " +
           "ORDER BY w.lastTransactionAt DESC")
    Page<Wallet> findRecentlyActiveWallets(@Param("sinceDate") LocalDateTime sinceDate, Pageable pageable);

    /**
     * Find wallets created within a date range.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @param pageable pagination parameters
     * @return page of wallets created in the date range
     */
    @Query("SELECT w FROM Wallet w WHERE w.createdAt BETWEEN :startDate AND :endDate " +
           "AND w.deleted = false")
    Page<Wallet> findWalletsCreatedBetween(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate,
                                            Pageable pageable);

    // ==================== Statistics & Reporting Methods ====================

    /**
     * Count wallets by status.
     *
     * @param status the wallet status
     * @return count of wallets
     */
    @Query("SELECT COUNT(w) FROM Wallet w WHERE w.status = :status AND w.deleted = false")
    long countByStatus(@Param("status") Wallet.WalletStatus status);

    /**
     * Count wallets by type.
     *
     * @param walletType the wallet type
     * @return count of wallets
     */
    @Query("SELECT COUNT(w) FROM Wallet w WHERE w.walletType = :walletType AND w.deleted = false")
    long countByWalletType(@Param("walletType") Wallet.WalletType walletType);

    /**
     * Count wallets by currency.
     *
     * @param currency the currency code
     * @return count of wallets
     */
    @Query("SELECT COUNT(w) FROM Wallet w WHERE w.currency = :currency AND w.deleted = false")
    long countByCurrency(@Param("currency") String currency);

    /**
     * Get wallet distribution by type.
     *
     * @return list of Object arrays containing [walletType, count]
     */
    @Query("SELECT w.walletType, COUNT(w) FROM Wallet w WHERE w.deleted = false GROUP BY w.walletType")
    List<Object[]> getWalletDistributionByType();

    /**
     * Get wallet distribution by currency.
     *
     * @return list of Object arrays containing [currency, count, totalBalance]
     */
    @Query("SELECT w.currency, COUNT(w), SUM(w.balance) FROM Wallet w " +
           "WHERE w.deleted = false GROUP BY w.currency")
    List<Object[]> getWalletDistributionByCurrency();

    /**
     * Get wallet statistics by status.
     *
     * @return list of Object arrays containing [status, count, totalBalance]
     */
    @Query("SELECT w.status, COUNT(w), SUM(w.balance) FROM Wallet w " +
           "WHERE w.deleted = false GROUP BY w.status")
    List<Object[]> getWalletStatsByStatus();

    /**
     * Calculate total balance across all wallets in a currency.
     *
     * @param currency the currency code
     * @return total balance
     */
    @Query("SELECT COALESCE(SUM(w.balance), 0) FROM Wallet w " +
           "WHERE w.currency = :currency AND w.deleted = false")
    BigDecimal calculateTotalBalanceByCurrency(@Param("currency") String currency);

    /**
     * Calculate total held balance across all wallets.
     * Important for liquidity management.
     *
     * @return total held balance
     */
    @Query("SELECT COALESCE(SUM(w.heldBalance), 0) FROM Wallet w WHERE w.deleted = false")
    BigDecimal calculateTotalHeldBalance();

    /**
     * Count active wallets.
     *
     * @return count of active wallets
     */
    @Query("SELECT COUNT(w) FROM Wallet w WHERE w.status = com.frankie.fintech.entity.Wallet.WalletStatus.ACTIVE " +
           "AND w.deleted = false")
    long countActiveWallets();

    /**
     * Count frozen wallets.
     *
     * @return count of frozen wallets
     */
    @Query("SELECT COUNT(w) FROM Wallet w WHERE w.isFrozen = true AND w.deleted = false")
    long countFrozenWallets();

    // ==================== Compliance & Audit Methods ====================

    /**
     * Find wallets exceeding daily limits.
     * Requires external calculation of daily spending.
     *
     * @param pageable pagination parameters
     * @return page of wallets with daily limits set
     */
    @Query("SELECT w FROM Wallet w WHERE w.dailyLimit IS NOT NULL " +
           "AND w.deleted = false")
    Page<Wallet> findWalletsWithDailyLimits(Pageable pageable);

    /**
     * Find all wallets for a user (including deleted) for audit purposes.
     *
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return page of all user wallets (including deleted)
     */
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId")
    Page<Wallet> findAllWalletsByUserIdForAudit(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find wallets with balance integrity issues.
     * Balance should equal availableBalance + heldBalance.
     *
     * @param pageable pagination parameters
     * @return page of wallets with potential balance issues
     */
    @Query("SELECT w FROM Wallet w WHERE w.balance != (w.availableBalance + w.heldBalance) " +
           "AND w.deleted = false")
    Page<Wallet> findWalletsWithBalanceIntegrityIssues(Pageable pageable);

    /**
     * Find wallets modified after a certain date.
     * Useful for audit and reconciliation.
     *
     * @param afterDate the date to check
     * @param pageable pagination parameters
     * @return page of modified wallets
     */
    @Query("SELECT w FROM Wallet w WHERE w.updatedAt > :afterDate")
    Page<Wallet> findWalletsModifiedAfter(@Param("afterDate") LocalDateTime afterDate, Pageable pageable);

    // ==================== Bulk Operations ====================

    /**
     * Find all wallets by IDs (bulk lookup).
     *
     * @param ids list of wallet IDs
     * @return list of found wallets
     */
    @Query("SELECT w FROM Wallet w WHERE w.id IN :ids AND w.deleted = false")
    List<Wallet> findAllByIds(@Param("ids") List<UUID> ids);

    /**
     * Bulk freeze wallets.
     *
     * @param walletIds list of wallet IDs to freeze
     * @param reason the reason for freezing
     * @param frozenBy who froze the wallets
     * @param frozenAt the freeze timestamp
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE Wallet w SET w.isFrozen = true, w.freezeReason = :reason, " +
           "w.frozenBy = :frozenBy, w.frozenAt = :frozenAt, " +
           "w.status = com.frankie.fintech.entity.Wallet.WalletStatus.SUSPENDED " +
           "WHERE w.id IN :walletIds AND w.deleted = false")
    int bulkFreezeWallets(@Param("walletIds") List<UUID> walletIds,
                          @Param("reason") String reason,
                          @Param("frozenBy") String frozenBy,
                          @Param("frozenAt") LocalDateTime frozenAt);

    /**
     * Bulk unfreeze wallets.
     *
     * @param walletIds list of wallet IDs to unfreeze
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE Wallet w SET w.isFrozen = false, w.freezeReason = NULL, " +
           "w.frozenBy = NULL, w.frozenAt = NULL, " +
           "w.status = com.frankie.fintech.entity.Wallet.WalletStatus.ACTIVE " +
           "WHERE w.id IN :walletIds AND w.deleted = false")
    int bulkUnfreezeWallets(@Param("walletIds") List<UUID> walletIds);

    /**
     * Bulk soft delete wallets.
     *
     * @param walletIds list of wallet IDs to delete
     * @param deletedBy who deleted the wallets
     * @param deletedAt the deletion timestamp
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE Wallet w SET w.deleted = true, w.deletedAt = :deletedAt, " +
           "w.deletedBy = :deletedBy, w.status = com.frankie.fintech.entity.Wallet.WalletStatus.CLOSED " +
           "WHERE w.id IN :walletIds AND w.deleted = false")
    int bulkSoftDeleteWallets(@Param("walletIds") List<UUID> walletIds,
                              @Param("deletedBy") String deletedBy,
                              @Param("deletedAt") LocalDateTime deletedAt);

    // ==================== Custom Projections for Performance ====================

    /**
     * Lightweight wallet projection for listing operations.
     */
    interface WalletListProjection {
        UUID getId();
        Wallet.WalletType getWalletType();
        String getCurrency();
        BigDecimal getBalance();
        BigDecimal getAvailableBalance();
        Wallet.WalletStatus getStatus();
        Boolean getIsFrozen();
        Boolean getIsDefault();
        LocalDateTime getLastTransactionAt();
    }

    /**
     * Find user's wallets with lightweight projection.
     *
     * @param userId the user ID
     * @return list of wallet projections
     */
    @Query("SELECT w.id as id, w.walletType as walletType, w.currency as currency, " +
           "w.balance as balance, w.availableBalance as availableBalance, " +
           "w.status as status, w.isFrozen as isFrozen, w.isDefault as isDefault, " +
           "w.lastTransactionAt as lastTransactionAt " +
           "FROM Wallet w WHERE w.user.id = :userId AND w.deleted = false")
    List<WalletListProjection> findWalletsByUserIdLightweight(@Param("userId") UUID userId);

    /**
     * Find all wallets with lightweight projection.
     *
     * @param pageable pagination parameters
     * @return page of wallet projections
     */
    @Query("SELECT w.id as id, w.walletType as walletType, w.currency as currency, " +
           "w.balance as balance, w.availableBalance as availableBalance, " +
           "w.status as status, w.isFrozen as isFrozen, w.isDefault as isDefault, " +
           "w.lastTransactionAt as lastTransactionAt " +
           "FROM Wallet w WHERE w.deleted = false")
    Page<WalletListProjection> findAllWalletsLightweight(Pageable pageable);
}
