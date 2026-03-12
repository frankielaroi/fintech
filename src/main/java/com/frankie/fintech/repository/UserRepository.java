package com.frankie.fintech.repository;


import com.frankie.fintech.entity.User;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User entity operations.
 * This repository provides production-ready methods for user management in a fintech application,
 * including security features, soft delete support, and audit trail queries.
 *
 * <p>All queries respect soft delete flag and account status unless explicitly stated otherwise.
 *
 * @author Fintech Team
 * @version 1.0
 * @since 2026-03-08
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // ==================== Basic Lookup Methods ====================

    /**
     * Find an active (non-deleted) user by email address.
     * This is the primary method for user authentication.
     *
     * @param email the user's email address
     * @return Optional containing the user if found and not deleted
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deleted = false")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * Find an active (non-deleted) user by phone number.
     *
     * @param phoneNumber the user's phone number
     * @return Optional containing the user if found and not deleted
     */
    @Query("SELECT u FROM User u WHERE u.phoneNumber = :phoneNumber AND u.deleted = false")
    Optional<User> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    /**
     * Find user by ID with pessimistic write lock for concurrent operations.
     * Use this when updating user data to prevent race conditions.
     *
     * @param id the user's unique identifier
     * @return Optional containing the locked user if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deleted = false")
    Optional<User> findByIdWithLock(@Param("id") UUID id);

    /**
     * Find user by email with pessimistic write lock.
     * Use this for critical operations like password reset or account updates.
     *
     * @param email the user's email address
     * @return Optional containing the locked user if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deleted = false")
    Optional<User> findByEmailWithLock(@Param("email") String email);

    // ==================== Authentication & Security Methods ====================

    /**
     * Find an active user by email for authentication purposes.
     * Only returns users with ACTIVE status who are not locked or deleted.
     *
     * @param email the user's email address
     * @return Optional containing the user if eligible for login
     */
    @Query("SELECT u FROM User u WHERE u.email = :email " +
           "AND u.deleted = false " +
           "AND u.status = com.frankie.fintech.entity.User.UserStatus.ACTIVE " +
           "AND u.accountLocked = false")
    Optional<User> findActiveUserByEmail(@Param("email") String email);

    /**
     * Find user by email including deleted users.
     * Use only for administrative purposes and audit trails.
     *
     * @param email the user's email address
     * @return Optional containing the user regardless of deletion status
     */
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailIncludingDeleted(@Param("email") String email);

    /**
     * Check if an email is already registered (excluding deleted accounts).
     * Useful for registration validation.
     *
     * @param email the email to check
     * @return true if email exists and is not deleted
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.deleted = false")
    boolean existsByEmail(@Param("email") String email);

    /**
     * Check if a phone number is already registered (excluding deleted accounts).
     *
     * @param phoneNumber the phone number to check
     * @return true if phone number exists and is not deleted
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.phoneNumber = :phoneNumber AND u.deleted = false")
    boolean existsByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    // ==================== Status & Verification Methods ====================

    /**
     * Find all users by account status with pagination.
     *
     * @param status the user status to filter by
     * @param pageable pagination parameters
     * @return page of users with the specified status
     */
    @Query("SELECT u FROM User u WHERE u.status = :status AND u.deleted = false")
    Page<User> findByStatus(@Param("status") User.UserStatus status, Pageable pageable);

    /**
     * Find all users by KYC level with pagination.
     * Important for compliance reporting.
     *
     * @param kycLevel the KYC level to filter by
     * @param pageable pagination parameters
     * @return page of users with the specified KYC level
     */
    @Query("SELECT u FROM User u WHERE u.kycLevel = :kycLevel AND u.deleted = false")
    Page<User> findByKycLevel(@Param("kycLevel") User.KycLevel kycLevel, Pageable pageable);

    /**
     * Find users pending email verification.
     *
     * @param pageable pagination parameters
     * @return page of users with unverified emails
     */
    @Query("SELECT u FROM User u WHERE u.emailVerified = false AND u.deleted = false")
    Page<User> findUsersWithUnverifiedEmail(Pageable pageable);

    /**
     * Find users pending phone verification.
     *
     * @param pageable pagination parameters
     * @return page of users with unverified phone numbers
     */
    @Query("SELECT u FROM User u WHERE u.phoneVerified = false AND u.deleted = false")
    Page<User> findUsersWithUnverifiedPhone(Pageable pageable);

    /**
     * Find all locked accounts for security review.
     *
     * @param pageable pagination parameters
     * @return page of locked user accounts
     */
    @Query("SELECT u FROM User u WHERE u.accountLocked = true AND u.deleted = false")
    Page<User> findLockedAccounts(Pageable pageable);

    // ==================== Update Methods ====================

    /**
     * Update user's last login information.
     * This is a bulk update operation for performance.
     *
     * @param userId the user's ID
     * @param loginTime the login timestamp
     * @param ipAddress the login IP address
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime, u.lastLoginIp = :ipAddress, " +
           "u.failedLoginAttempts = 0 WHERE u.id = :userId")
    int updateLastLogin(@Param("userId") UUID userId,
                        @Param("loginTime") LocalDateTime loginTime,
                        @Param("ipAddress") String ipAddress);

    /**
     * Increment failed login attempts counter.
     *
     * @param userId the user's ID
     * @param attemptTime the time of the failed attempt
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1, " +
           "u.lastFailedLoginAt = :attemptTime WHERE u.id = :userId")
    int incrementFailedLoginAttempts(@Param("userId") UUID userId,
                                      @Param("attemptTime") LocalDateTime attemptTime);

    /**
     * Lock a user account.
     *
     * @param userId the user's ID
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE User u SET u.accountLocked = true, " +
           "u.status = com.frankie.fintech.entity.User.UserStatus.LOCKED " +
           "WHERE u.id = :userId")
    int lockAccount(@Param("userId") UUID userId);

    /**
     * Unlock a user account.
     *
     * @param userId the user's ID
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE User u SET u.accountLocked = false, " +
           "u.status = com.frankie.fintech.entity.User.UserStatus.ACTIVE, " +
           "u.failedLoginAttempts = 0 WHERE u.id = :userId")
    int unlockAccount(@Param("userId") UUID userId);

    /**
     * Mark email as verified.
     *
     * @param userId the user's ID
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true WHERE u.id = :userId")
    int markEmailAsVerified(@Param("userId") UUID userId);

    /**
     * Mark phone as verified.
     *
     * @param userId the user's ID
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE User u SET u.phoneVerified = true WHERE u.id = :userId")
    int markPhoneAsVerified(@Param("userId") UUID userId);

    /**
     * Update user's KYC level.
     *
     * @param userId the user's ID
     * @param kycLevel the new KYC level
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE User u SET u.kycLevel = :kycLevel WHERE u.id = :userId")
    int updateKycLevel(@Param("userId") UUID userId, @Param("kycLevel") User.KycLevel kycLevel);

    /**
     * Soft delete a user account.
     *
     * @param userId the user's ID
     * @param deletedBy the identifier of who deleted the account
     * @param deletedAt the deletion timestamp
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE User u SET u.deleted = true, u.deletedAt = :deletedAt, " +
           "u.deletedBy = :deletedBy, u.status = com.frankie.fintech.entity.User.UserStatus.CLOSED " +
           "WHERE u.id = :userId")
    int softDeleteUser(@Param("userId") UUID userId,
                       @Param("deletedBy") String deletedBy,
                       @Param("deletedAt") LocalDateTime deletedAt);

    // ==================== Compliance & Audit Methods ====================

    /**
     * Find users with expired passwords that need to be reset.
     * Critical for compliance with password rotation policies.
     *
     * @param currentTime the current timestamp
     * @param pageable pagination parameters
     * @return page of users with expired passwords
     */
    @Query("SELECT u FROM User u WHERE u.passwordExpiresAt < :currentTime " +
           "AND u.deleted = false AND u.status = com.frankie.fintech.entity.User.UserStatus.ACTIVE")
    Page<User> findUsersWithExpiredPasswords(@Param("currentTime") LocalDateTime currentTime,
                                              Pageable pageable);

    /**
     * Find users who haven't logged in for a specified period.
     * Useful for inactive account cleanup and security audits.
     *
     * @param cutoffDate the date threshold for inactivity
     * @param pageable pagination parameters
     * @return page of inactive users
     */
    @Query("SELECT u FROM User u WHERE (u.lastLoginAt IS NULL OR u.lastLoginAt < :cutoffDate) " +
           "AND u.deleted = false AND u.status = com.frankie.fintech.entity.User.UserStatus.ACTIVE")
    Page<User> findInactiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);

    /**
     * Find users with multiple failed login attempts.
     * Important for detecting potential security threats.
     *
     * @param threshold minimum number of failed attempts
     * @param pageable pagination parameters
     * @return page of users with failed login attempts exceeding threshold
     */
    @Query("SELECT u FROM User u WHERE u.failedLoginAttempts >= :threshold " +
           "AND u.deleted = false ORDER BY u.failedLoginAttempts DESC")
    Page<User> findUsersWithFailedLoginAttempts(@Param("threshold") Integer threshold,
                                                 Pageable pageable);

    /**
     * Find users created within a date range.
     * Useful for analytics and compliance reporting.
     *
     * @param startDate the start of the date range
     * @param endDate the end of the date range
     * @param pageable pagination parameters
     * @return page of users created within the date range
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate " +
           "AND u.deleted = false")
    Page<User> findUsersCreatedBetween(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        Pageable pageable);

    /**
     * Find users by role.
     *
     * @param role the user role to filter by
     * @param pageable pagination parameters
     * @return page of users with the specified role
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r = :role AND u.deleted = false")
    Page<User> findByRole(@Param("role") User.UserRole role, Pageable pageable);

    /**
     * Count users by status.
     * Useful for dashboard metrics.
     *
     * @param status the user status
     * @return count of users with the specified status
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status AND u.deleted = false")
    long countByStatus(@Param("status") User.UserStatus status);

    /**
     * Count users by KYC level.
     * Critical for compliance reporting.
     *
     * @param kycLevel the KYC level
     * @return count of users with the specified KYC level
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.kycLevel = :kycLevel AND u.deleted = false")
    long countByKycLevel(@Param("kycLevel") User.KycLevel kycLevel);

    // ==================== Advanced Search Methods ====================

    /**
     * Search users by name (case-insensitive partial match).
     *
     * @param searchTerm the search term
     * @param pageable pagination parameters
     * @return page of matching users
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND u.deleted = false")
    Page<User> searchByName(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find fully verified users (email and phone verified, active status).
     * These users can perform full transactions.
     *
     * @param pageable pagination parameters
     * @return page of fully verified users
     */
    @Query("SELECT u FROM User u WHERE u.emailVerified = true " +
           "AND u.phoneVerified = true " +
           "AND u.status = com.frankie.fintech.entity.User.UserStatus.ACTIVE " +
           "AND u.accountLocked = false " +
           "AND u.deleted = false")
    Page<User> findFullyVerifiedUsers(Pageable pageable);

    /**
     * Find users who require KYC verification (basic level or none).
     *
     * @param pageable pagination parameters
     * @return page of users requiring KYC
     */
    @Query("SELECT u FROM User u WHERE u.kycLevel IN (com.frankie.fintech.entity.User.KycLevel.NONE, com.frankie.fintech.entity.User.KycLevel.BASIC) " +
           "AND u.status = com.frankie.fintech.entity.User.UserStatus.ACTIVE " +
           "AND u.deleted = false")
    Page<User> findUsersRequiringKyc(Pageable pageable);

    /**
     * Find users who have 2FA enabled.
     *
     * @param pageable pagination parameters
     * @return page of users with 2FA enabled
     */
    @Query("SELECT u FROM User u WHERE u.twoFactorEnabled = true AND u.deleted = false")
    Page<User> findUsersWithTwoFactorEnabled(Pageable pageable);

    /**
     * Find users without 2FA for security campaigns.
     *
     * @param kycLevel minimum KYC level to include
     * @param pageable pagination parameters
     * @return page of users without 2FA
     */
    @Query("SELECT u FROM User u WHERE u.twoFactorEnabled = false " +
           "AND u.kycLevel >= :kycLevel " +
           "AND u.status = com.frankie.fintech.entity.User.UserStatus.ACTIVE " +
           "AND u.deleted = false")
    Page<User> findUsersWithoutTwoFactor(@Param("kycLevel") User.KycLevel kycLevel,
                                          Pageable pageable);

    // ==================== Batch Operations ====================

    /**
     * Find all users by IDs (bulk lookup).
     * Excludes soft-deleted users.
     *
     * @param ids list of user IDs
     * @return list of found users
     */
    @Query("SELECT u FROM User u WHERE u.id IN :ids AND u.deleted = false")
    List<User> findAllByIds(@Param("ids") List<UUID> ids);

    /**
     * Lock multiple accounts in bulk.
     * Use for emergency security responses.
     *
     * @param userIds list of user IDs to lock
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE User u SET u.accountLocked = true, " +
           "u.status = com.frankie.fintech.entity.User.UserStatus.LOCKED " +
           "WHERE u.id IN :userIds AND u.deleted = false")
    int lockAccountsBulk(@Param("userIds") List<UUID> userIds);

    /**
     * Bulk soft delete users.
     *
     * @param userIds list of user IDs to delete
     * @param deletedBy the identifier of who performed the deletion
     * @param deletedAt the deletion timestamp
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE User u SET u.deleted = true, u.deletedAt = :deletedAt, " +
           "u.deletedBy = :deletedBy, u.status = com.frankie.fintech.entity.User.UserStatus.CLOSED " +
           "WHERE u.id IN :userIds AND u.deleted = false")
    int softDeleteUsersBulk(@Param("userIds") List<UUID> userIds,
                            @Param("deletedBy") String deletedBy,
                            @Param("deletedAt") LocalDateTime deletedAt);

    // ==================== Statistics & Reporting Methods ====================

    /**
     * Count total active users.
     *
     * @return count of active, non-deleted users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = com.frankie.fintech.entity.User.UserStatus.ACTIVE " +
           "AND u.deleted = false")
    long countActiveUsers();

    /**
     * Count users registered today.
     *
     * @param startOfDay the start of the current day
     * @return count of users registered today
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startOfDay AND u.deleted = false")
    long countUsersRegisteredToday(@Param("startOfDay") LocalDateTime startOfDay);

    /**
     * Get user registration statistics by date range.
     * Returns count grouped by status.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return list of Object arrays containing [status, count]
     */
    @Query("SELECT u.status, COUNT(u) FROM User u " +
           "WHERE u.createdAt BETWEEN :startDate AND :endDate " +
           "AND u.deleted = false " +
           "GROUP BY u.status")
    List<Object[]> getUserRegistrationStatsByStatus(@Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Get KYC distribution statistics.
     *
     * @return list of Object arrays containing [kycLevel, count]
     */
    @Query("SELECT u.kycLevel, COUNT(u) FROM User u WHERE u.deleted = false GROUP BY u.kycLevel")
    List<Object[]> getKycDistributionStats();

    // ==================== Security Monitoring Methods ====================

    /**
     * Find users with suspicious activity (multiple failed logins in a time window).
     *
     * @param threshold number of failed attempts threshold
     * @param timeWindow time window to check
     * @return list of suspicious users
     */
    @Query("SELECT u FROM User u WHERE u.failedLoginAttempts >= :threshold " +
           "AND u.lastFailedLoginAt >= :timeWindow " +
           "AND u.deleted = false")
    List<User> findSuspiciousUsers(@Param("threshold") Integer threshold,
                                    @Param("timeWindow") LocalDateTime timeWindow);

    /**
     * Find accounts that should be auto-locked due to excessive failed attempts.
     *
     * @param maxAttempts maximum allowed failed attempts
     * @return list of users that should be locked
     */
    @Query("SELECT u FROM User u WHERE u.failedLoginAttempts >= :maxAttempts " +
           "AND u.accountLocked = false " +
           "AND u.deleted = false")
    List<User> findAccountsToAutoLock(@Param("maxAttempts") Integer maxAttempts);

    // ==================== Cleanup & Maintenance Methods ====================

    /**
     * Find stale unverified accounts for cleanup.
     * These are accounts that were created but never verified within the grace period.
     *
     * @param cutoffDate the cutoff date for cleanup
     * @return list of stale unverified accounts
     */
    @Query("SELECT u FROM User u WHERE u.status = com.frankie.fintech.entity.User.UserStatus.PENDING_VERIFICATION " +
           "AND u.emailVerified = false " +
           "AND u.createdAt < :cutoffDate " +
           "AND u.deleted = false")
    List<User> findStaleUnverifiedAccounts(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Reset failed login attempts for users after a cooling period.
     *
     * @param cutoffTime the time before which failed attempts should be reset
     * @return number of records updated
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0 " +
           "WHERE u.lastFailedLoginAt < :cutoffTime " +
           "AND u.failedLoginAttempts > 0 " +
           "AND u.deleted = false")
    int resetOldFailedLoginAttempts(@Param("cutoffTime") LocalDateTime cutoffTime);

    // ==================== Custom Projections for Performance ====================

    /**
     * Lightweight user projection for listing/searching operations.
     * Reduces data transfer and improves query performance.
     */
    interface UserListProjection {
        UUID getId();
        String getName();
        String getEmail();
        User.UserStatus getStatus();
        User.KycLevel getKycLevel();
        LocalDateTime getCreatedAt();
        LocalDateTime getLastLoginAt();
    }

    /**
     * Find users with lightweight projection for list views.
     *
     * @param pageable pagination parameters
     * @return page of user projections
     */
    @Query("SELECT u.id as id, u.name as name, u.email as email, " +
           "u.status as status, u.kycLevel as kycLevel, " +
           "u.createdAt as createdAt, u.lastLoginAt as lastLoginAt " +
           "FROM User u WHERE u.deleted = false")
    Page<UserListProjection> findAllUsersLightweight(Pageable pageable);

    /**
     * Search users with lightweight projection.
     *
     * @param searchTerm the search term
     * @param pageable pagination parameters
     * @return page of matching user projections
     */
    @Query("SELECT u.id as id, u.name as name, u.email as email, " +
           "u.status as status, u.kycLevel as kycLevel, " +
           "u.createdAt as createdAt, u.lastLoginAt as lastLoginAt " +
           "FROM User u WHERE (LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND u.deleted = false")
    Page<UserListProjection> searchUsersLightweight(@Param("searchTerm") String searchTerm,
                                                     Pageable pageable);

}
