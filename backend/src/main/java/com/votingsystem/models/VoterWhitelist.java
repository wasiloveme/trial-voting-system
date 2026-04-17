package com.votingsystem.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * =============================================================================
 * FILE: VoterWhitelist.java
 * PACKAGE: com.votingsystem.models
 * =============================================================================
 *
 * <h2>IAS101 "SECURED SUPER SYSTEM" — RUBRIC MAPPING</h2>
 * <ul>
 * <li><b>[SUBSYSTEM 1 — IAM: Registration Pipeline Gatekeeper]:</b>
 * This entity is the first and hardest security checkpoint in the
 * registration pipeline. Before a student can even view the sign-up
 * form, the {@code RegistrationService} performs a
 * {@code whitelistRepository.existsByStudentId(studentId)} check.
 * If the student ID is NOT in this table, they receive an immediate
 * hard stop with a message directing them to the SSC/Registrar.
 * This prevents unauthorized individuals from creating voting accounts
 * even if they obtain the registration URL.</li>
 * <li><b>[SUBSYSTEM 4 — Transaction/Operations: Admin Whitelist
 * Management]:</b>
 * Admins populate this table via the Voter Registry panel — either
 * one entry at a time or via bulk CSV import processed by
 * {@code WhitelistService.bulkImport()}.</li>
 * <li><b>[ANTI-DOUBLE-REGISTRATION]:</b> The {@code isRegistered} field
 * flips to {@code TRUE} atomically when a student completes OTP
 * verification. Any subsequent registration attempt for the same
 * student ID is rejected by a pre-check on this boolean field,
 * preventing duplicate account creation.</li>
 * <li><b>[AUDIT TRAIL]:</b> Inherits {@code createdAt}/{@code updatedAt}
 * from {@code BaseAuditEntity}, recording when each ID was whitelisted
 * and when the registration status changed.</li>
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
@Table(name = "voter_whitelist")
public class VoterWhitelist extends BaseAuditEntity {

    // =========================================================================
    // SECTION 1: PRIMARY KEY
    // =========================================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // SECTION 2: WHITELIST ENTRY
    // [SUBSYSTEM 1 — IAM: Registration Pipeline Gate]
    // =========================================================================

    /**
     * <b>[WHITELIST GATE — Student Identifier]</b>
     * The university-assigned Student ID that has been pre-approved by the
     * SSC or Registrar for voter registration. The {@code unique = true}
     * constraint at the JPA and DB level prevents duplicate whitelist entries,
     * which would cause inconsistent {@code isRegistered} state if two rows
     * existed for the same student ID.
     *
     * <p>
     * This value is compared against {@code User.studentId} during
     * registration Step 1. The comparison is case-sensitive and uses
     * exact-match equality — no wildcards or fuzzy matching.
     * </p>
     */
    @Column(name = "student_id", nullable = false, unique = true, length = 20)
    private String studentId;

    /**
     * <b>[ANTI-DOUBLE-REGISTRATION LOCK]</b>
     * Tracks whether this whitelisted student has already completed the
     * full registration pipeline (including OTP verification).
     *
     * <ul>
     * <li>{@code FALSE} (default) — Pre-approved but not yet registered.
     * Allowed to proceed through the sign-up form.</li>
     * <li>{@code TRUE} — Registration completed. Any new registration
     * attempt with this student ID is immediately rejected by the
     * {@code RegistrationService} with an appropriate error message,
     * regardless of what data is submitted.</li>
     * </ul>
     *
     * This flag is set to {@code TRUE} atomically within the same
     * {@code @Transactional} block that creates the {@code User} entity
     * and confirms OTP verification, ensuring consistency between the
     * two tables even in the event of partial failures.
     */
    @Builder.Default
    @Column(name = "is_registered", nullable = false)
    private boolean isRegistered = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public boolean isRegistered() { return isRegistered; }
    public void setRegistered(boolean registered) { isRegistered = registered; }
}
