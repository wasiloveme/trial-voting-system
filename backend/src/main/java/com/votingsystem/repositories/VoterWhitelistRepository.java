package com.votingsystem.repositories;

import com.votingsystem.models.VoterWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * =============================================================================
 * FILE: VoterWhitelistRepository.java
 * PACKAGE: com.votingsystem.repositories
 * =============================================================================
 *
 * <h2>IAS101 "SECURED SUPER SYSTEM" — RUBRIC MAPPING</h2>
 * <ul>
 * <li><b>[SUBSYSTEM 1 — IAM: Registration Pipeline Gatekeeper]:</b>
 * {@code existsByStudentId()} is the single most important guard in
 * the registration flow. It is the FIRST database call made when any
 * student attempts to register. A {@code false} result is a hard stop
 * — the registration pipeline terminates immediately.</li>
 * <li><b>[SUBSYSTEM 4 — Transaction: Bulk CSV Import]:</b>
 * {@code saveAll()} (inherited from JpaRepository) is used by the
 * Admin's bulk CSV import feature in {@code WhitelistService}.</li>
 * </ul>
 *
 * @author Secured Super System — Lead Architect
 * @version 1.0
 */
@Repository
public interface VoterWhitelistRepository extends JpaRepository<VoterWhitelist, Long> {

    /**
     * <b>[REGISTRATION GATE — Step 1 Whitelist Check]</b>
     * The primary security check of the registration pipeline.
     * Called as the very first operation in
     * {@code AuthService.beginRegistration()}.
     * Returns {@code false} → hard stop with SSC/Registrar redirect message.
     * Returns {@code true} → student may proceed to the sign-up form.
     *
     * @param studentId The Student ID entered by the student on the
     *                  registration entry screen.
     * @return {@code true} if this Student ID is on the pre-approved whitelist.
     */
    boolean existsByStudentId(String studentId);

    /**
     * <b>[REGISTRATION GATE — Full Entry Lookup]</b>
     * Fetches the full whitelist entry to check BOTH existence AND
     * {@code isRegistered} status in one query. Called when a student
     * attempts to register to prevent re-registration of an already-active
     * student ID.
     *
     * @param studentId The Student ID to look up.
     * @return The whitelist entry if found, empty if not whitelisted.
     */
    Optional<VoterWhitelist> findByStudentId(String studentId);

    /**
     * <b>[ADMIN — Whitelist Registry Management]</b>
     * Returns all un-registered whitelist entries. Used by the Admin's
     * Voter Registry panel to show which pre-approved students have not
     * yet created an account.
     *
     * @return All whitelist entries where {@code isRegistered = false}.
     */
    List<VoterWhitelist> findByIsRegisteredFalse();

    /**
     * <b>[ANTI-DOUBLE-REGISTRATION LOCK — Mark as Registered]</b>
     * Called atomically within the same {@code @Transactional} block as
     * User creation and OTP confirmation. Flips {@code isRegistered} to
     * {@code true}, preventing any future registration attempt with the
     * same Student ID from proceeding past Step 1.
     *
     * @param studentId The Student ID that has just completed registration.
     */
    @Modifying
    @Query("UPDATE VoterWhitelist w SET w.isRegistered = true WHERE w.studentId = :studentId")
    void markAsRegistered(@Param("studentId") String studentId);
}
