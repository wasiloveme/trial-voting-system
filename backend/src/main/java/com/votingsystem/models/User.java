package com.votingsystem.models;

import com.votingsystem.security.AesAttributeConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * =============================================================================
 * FILE: User.java
 * PACKAGE: com.votingsystem.models
 * =============================================================================
 *
 * <h2>IAS101 "SECURED SUPER SYSTEM" — RUBRIC MAPPING</h2>
 * <ul>
 * <li><b>[SUBSYSTEM 1 — Identity & Access Management (IAM)]:</b> This is the
 * central IAM entity. It stores credentials, the assigned {@code Role}
 * for RBAC enforcement, account lifecycle state ({@code AccountStatus}),
 * and MFA secrets required for the OTP authentication flow.</li>
 * <li><b>[ENCRYPTION AT REST — AES-256]:</b> The {@code address} and
 * {@code contactNumber} fields are decorated with
 * {@code @Convert(converter = AesAttributeConverter.class)}. This
 * transparently encrypts the value with AES-256 before writing to
 * PostgreSQL and decrypts it on read. Even if the database is
 * compromised, raw PII (Personally Identifiable Information) is
 * never exposed in plaintext — satisfying the IAS101 Encryption at
 * Rest requirement.</li>
 * <li><b>[RBAC ENGINE]:</b> The {@code Role} enum directly drives Spring
 * Security's authorization decisions. The JWT issued at login will
 * carry this role as a claim (e.g., "role": "ADMIN"), which the
 * {@code JwtAuthFilter} validates on every API call.</li>
 * <li><b>[AUDIT TRAIL]:</b> Inherits {@code createdAt} and {@code updatedAt}
 * from {@code BaseAuditEntity}, automatically recording every account
 * creation and modification event.</li>
 * <li><b>[2FA / OTP ENGINE]:</b> {@code otpCode} and {@code otpExpiry} fields
 * support the Notification & OTP subsystem. The {@code mfaSecret} field
 * is reserved for TOTP-based authenticator app integration.</li>
 * </ul>
 *
 * @author Secured Super System — Lead Architect
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "users")
public class User extends BaseAuditEntity {

    // =========================================================================
    // SECTION 1: PRIMARY KEY
    // =========================================================================

    /** Auto-incremented surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // SECTION 2: IAM & CREDENTIALS
    // [SUBSYSTEM 1 — Identity & Access Management]
    // =========================================================================

    /**
     * The university-assigned Student ID. Serves as the logical, human-readable
     * unique identifier. Used as the login username. {@code unique = true}
     * enforces a database-level uniqueness constraint preventing duplicate
     * account registration.
     */
    @Column(name = "student_id", nullable = false, unique = true, length = 20)
    private String studentId;

    /**
     * <b>[SECURITY — Hashed Credential]</b>
     * Stores the BCrypt-hashed password. Raw plaintext passwords are NEVER
     * stored. The {@code length = 60} is the exact output length of a BCrypt
     * hash, preventing column truncation vulnerabilities.
     */
    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    /**
     * <b>[RBAC ENGINE — Role Assignment]</b>
     * Stores the user's role as a String in the DB for readability. This value
     * is read by the {@code JwtTokenProvider} to embed the role claim into the
     * issued JWT, which the {@code JwtAuthFilter} then enforces on every
     * protected API request.
     *
     * <ul>
     * <li>USER — Standard voter privileges.</li>
     * <li>CANDIDATE — Inherits USER + campaign management access.</li>
     * <li>ADMIN — Full system control, separate login route.</li>
     * </ul>
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.USER;

    /**
     * <b>[IAM — Account Lifecycle State]</b>
     * Controls whether a user can log in and vote. The registration pipeline
     * sets this to {@code PENDING} on sign-up and only flips to {@code ACTIVE}
     * upon successful OTP email/SMS verification (2FA confirmation step).
     *
     * <ul>
     * <li>PENDING — Registered but OTP not yet confirmed.</li>
     * <li>ACTIVE — Verified and allowed to vote.</li>
     * <li>VOTED — Has cast a ballot; voting API locked for this user.</li>
     * <li>LOCKED — Admin-suspended; all access denied.</li>
     * </ul>
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus accountStatus = AccountStatus.PENDING;

    // =========================================================================
    // SECTION 3: DEMOGRAPHICS
    // =========================================================================

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "middle_name", length = 50)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "birthday", nullable = false)
    private LocalDate birthday;

    @Column(name = "section", length = 20)
    private String section;

    @Column(name = "program", length = 50)
    private String program;

    @Column(name = "year_level")
    private Integer yearLevel;

    // =========================================================================
    // SECTION 4: CONTACT DETAILS
    // [ENCRYPTION AT REST — AES-256 via AttributeConverter]
    // =========================================================================

    /**
     * Gmail address. Used by the Notification Engine to dispatch OTP codes
     * and system notifications. Stored as plaintext (not PII-critical) but
     * enforced unique at DB level.
     */
    @Column(name = "gmail", nullable = false, unique = true)
    private String gmail;

    /**
     * <b>[IAS101 — ENCRYPTION AT REST (AES-256)]</b>
     * The phone number is classified as PII. The {@code AesAttributeConverter}
     * intercepts the value before any DB write, encrypts it using AES-256-GCM
     * with a server-held secret key, and stores the Base64-encoded ciphertext.
     * On every database read, it is transparently decrypted back to plaintext
     * for application use. The raw column in PostgreSQL holds unreadable
     * ciphertext.
     */
    @Convert(converter = AesAttributeConverter.class)
    @Column(name = "contact_number", length = 512)
    private String contactNumber;

    /**
     * <b>[IAS101 — ENCRYPTION AT REST (AES-256)]</b>
     * Full residential address. Classified as sensitive PII. Encrypted with
     * the same {@code AesAttributeConverter} as {@code contactNumber}.
     * The column length is set to 512 to accommodate the longer Base64
     * ciphertext output from the AES encryption of a full address string.
     */
    @Convert(converter = AesAttributeConverter.class)
    @Column(name = "address", length = 512)
    private String address;

    // =========================================================================
    // SECTION 5: MFA / OTP FIELDS
    // [SUBSYSTEM 1 — IAM: 2FA & OTP Engine]
    // =========================================================================

    /**
     * <b>[OTP ENGINE]</b>
     * Stores the in-flight 6-digit OTP code generated by the
     * {@code OtpService}. This field is nulled out immediately after
     * successful verification to prevent replay attacks. {@code nullable = true}
     * because it only exists during active OTP flows.
     */
    @Column(name = "otp_code", length = 6)
    private String otpCode;

    /**
     * <b>[OTP ENGINE — Expiry Enforcement]</b>
     * Server-side expiry timestamp for the OTP. The {@code OtpService}
     * validates that {@code LocalDateTime.now().isBefore(otpExpiry)} before
     * accepting any OTP submission, enforcing the 5-minute expiry window and
     * preventing brute-force time-extension attacks.
     */
    @Column(name = "otp_expiry")
    private java.time.LocalDateTime otpExpiry;

    /**
     * <b>[MFA — TOTP Secret]</b>
     * Reserved for future TOTP (Time-based One-Time Password) authenticator
     * app integration (e.g., Google Authenticator). Stores the Base32-encoded
     * shared secret used to generate time-synchronized OTP codes.
     */
    @Column(name = "mfa_secret", length = 64)
    private String mfaSecret;

    // =========================================================================
    // SECTION 6: ENUMERATIONS (Inner Static Types)
    // =========================================================================

    /**
     * <b>[RBAC — Role Definitions]</b>
     * Defines the three hierarchical roles of the Secured Super System.
     * Used by Spring Security's {@code @PreAuthorize} annotations and the
     * {@code SecurityConfig} HTTP security rules.
     */
    public enum Role {
        /** Standard student voter. Access to dashboard, ballot, verification portal. */
        USER,
        /** Hybrid user. Inherits USER + unlocks campaign management endpoints. */
        CANDIDATE,
        /** System administrator. Full access via separate /admin route. */
        ADMIN
    }

    /**
     * <b>[IAM — Account Lifecycle]</b>
     * State machine for the user account, enforced at the service layer before
     * any sensitive operation (login, voting) is permitted.
     */
    public enum AccountStatus {
        PENDING,
        ACTIVE,
        VOTED,
        LOCKED
    }
}
