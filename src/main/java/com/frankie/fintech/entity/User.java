package com.frankie.fintech.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User entity representing a user account in the fintech system.
 * This entity includes security features, audit trails, and comprehensive validation
 * required for production fintech applications.
 */
@Entity
@Table(name = "users",
    indexes = {
        @Index(name = "idx_email", columnList = "email", unique = true),
        @Index(name = "idx_phone_number", columnList = "phone_number"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "created_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Password should be stored as a hashed value (e.g., using BCrypt).
     * Never store plain text passwords in production.
     */
    @NotBlank(message = "Password is required")
    @Column(name = "password", nullable = false)
    private String password;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be in valid E.164 format")
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /**
     * Account status for managing user lifecycle.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    /**
     * Email verification status - critical for fintech compliance.
     */
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    /**
     * Phone verification status - often required for 2FA.
     */
    @Column(name = "phone_verified", nullable = false)
    @Builder.Default
    private Boolean phoneVerified = false;

    /**
     * Two-factor authentication enablement.
     */
    @Column(name = "two_factor_enabled", nullable = false)
    @Builder.Default
    private Boolean twoFactorEnabled = false;

    /**
     * Secret key for TOTP-based 2FA (should be encrypted at rest).
     */
    @Column(name = "two_factor_secret", length = 100)
    private String twoFactorSecret;

    /**
     * KYC (Know Your Customer) verification level.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_level", nullable = false, length = 20)
    @Builder.Default
    private KycLevel kycLevel = KycLevel.NONE;

    /**
     * Account locked status for security purposes.
     */
    @Column(name = "account_locked", nullable = false)
    @Builder.Default
    private Boolean accountLocked = false;

    /**
     * Number of failed login attempts - for security monitoring.
     */
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    /**
     * Last failed login timestamp.
     */
    @Column(name = "last_failed_login_at")
    private LocalDateTime lastFailedLoginAt;

    /**
     * Last successful login timestamp.
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * Last login IP address for security auditing.
     */
    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    /**
     * Password expiry date - many fintech regulations require periodic password changes.
     */
    @Column(name = "password_expires_at")
    private LocalDateTime passwordExpiresAt;

    /**
     * Soft delete flag - never hard delete user data in fintech for audit purposes.
     */
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    /**
     * Timestamp when the user was soft deleted.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Audit trail: User who deleted this account.
     */
    @Column(name = "deleted_by")
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
     * User roles for authorization.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        indexes = @Index(name = "idx_user_roles_user_id", columnList = "user_id")
    )
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private java.util.Set<UserRole> roles = new java.util.HashSet<>();

    /**
     * Enum defining possible user account statuses.
     */
    public enum UserStatus {
        PENDING_VERIFICATION,
        ACTIVE,
        SUSPENDED,
        LOCKED,
        CLOSED
    }

    /**
     * Enum defining KYC verification levels.
     */
    public enum KycLevel {
        NONE,
        BASIC,
        INTERMEDIATE,
        ADVANCED
    }

    /**
     * Enum defining user roles.
     */
    public enum UserRole {
        ROLE_USER,
        ROLE_ADMIN,
        ROLE_SUPPORT,
        ROLE_COMPLIANCE
    }

    /**
     * Check if the user account is active and not locked.
     */
    public boolean isAccountActive() {
        return status == UserStatus.ACTIVE
            && !accountLocked
            && !deleted
            && emailVerified;
    }

    /**
     * Check if the user can perform transactions based on KYC level.
     */
    public boolean canPerformTransactions() {
        return isAccountActive() && kycLevel != KycLevel.NONE;
    }

    /**
     * Mark the account as deleted (soft delete).
     */
    public void markAsDeleted(String deletedByUser) {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedByUser;
        this.status = UserStatus.CLOSED;
    }

    /**
     * Record a failed login attempt.
     */
    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        this.lastFailedLoginAt = LocalDateTime.now();
    }

    /**
     * Record a successful login.
     */
    public void recordSuccessfulLogin(String ipAddress) {
        this.failedLoginAttempts = 0;
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ipAddress;
    }

    /**
     * Lock the account after too many failed attempts.
     */
    public void lockAccount() {
        this.accountLocked = true;
        this.status = UserStatus.LOCKED;
    }
}
