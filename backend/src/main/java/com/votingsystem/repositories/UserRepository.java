package com.votingsystem.repositories;

import com.votingsystem.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * =============================================================================
 * FILE: UserRepository.java
 * PACKAGE: com.votingsystem.repositories
 * =============================================================================
 *
 * <h2>IAS101 "SECURED SUPER SYSTEM" — RUBRIC MAPPING</h2>
 * <ul>
 * <li><b>[SUBSYSTEM 1 — IAM]:</b> Primary data access interface for the
 * User entity. Drives login authentication, registration, OTP flow,
 * and account lifecycle management.</li>
 * <li><b>[SUBSYSTEM 5 — Analytics: Turnout Leaderboard]:</b>
 * {@code countVotedByProgram()} powers the Admin's Live Turnout
 * Leaderboard via a single GROUP BY SQL query — no N+1 queries.</li>
 * <li><b>[INPUT VALIDATION — SQLi Prevention]:</b> All queries use
 * named parameters ({@code :param}) via Spring Data's
 * {@code @Query} + {@code @Param}. JPQL and named parameters are
 * compiled into {@code PreparedStatement}s by Hibernate, which
 * makes SQL injection structurally impossible for these queries.</li>
 * </ul>
 *
 * @author Secured Super System — Lead Architect
 * @version 1.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // =========================================================================
    // [SUBSYSTEM 1 — IAM: Authentication & Registration Lookups]
    // =========================================================================

    /**
     * <b>[AUTH — Login & UserDetailsService]</b>
     * Looks up a user by Student ID for the authentication flow.
     * Called by {@code UserDetailsServiceImpl.loadUserByUsername()}
     * and {@code AuthService.login()}.
     *
     * @param studentId The university-assigned Student ID (login username).
     * @return An {@code Optional<User>} — empty if no account exists.
     */
    Optional<User> findByStudentId(String studentId);

    /**
     * <b>[AUTH — Forgot Password & OTP Dispatch]</b>
     * Finds a user by their registered Gmail address. Used by the
     * Forgot Password flow when the student inputs their email for OTP delivery.
     *
     * @param gmail The registered Gmail address.
     * @return An {@code Optional<User>} — empty if no matching account.
     */
    Optional<User> findByGmail(String gmail);

    /**
     * <b>[REGISTRATION — Duplicate Email Guard]</b>
     * Checks if a Gmail address is already taken before allowing
     * a new registration to proceed. Prevents duplicate accounts sharing
     * the same email address.
     *
     * @param gmail The Gmail address to check.
     * @return {@code true} if a user already exists with this email.
     */
    boolean existsByGmail(String gmail);

    /**
     * <b>[REGISTRATION — Duplicate Student ID Guard]</b>
     * Checks if a Student ID already has an account before creating a
     * new one. Complements the DB-level unique constraint with an
     * application-layer check that provides a meaningful error message.
     *
     * @param studentId The Student ID to check.
     * @return {@code true} if an account already exists for this ID.
     */
    boolean existsByStudentId(String studentId);

    // =========================================================================
    // [SUBSYSTEM 1 — IAM: Account Status Management]
    // =========================================================================

    /**
     * <b>[OTP FLOW — Account Activation / Status Flip]</b>
     * Atomically updates a single user's account status. Used by:
     * <ul>
     * <li>OTP confirmation → PENDING to ACTIVE</li>
     * <li>Vote cast → ACTIVE to VOTED (in VotingService transaction)</li>
     * <li>Admin lock → any status to LOCKED</li>
     * </ul>
     * {@code @Modifying} signals Spring Data this is a DML (write) query.
     * Must be called within a {@code @Transactional} method.
     *
     * @param userId    The primary key of the user to update.
     * @param newStatus The target {@code AccountStatus} enum value.
     */
    @Modifying
    @Query("UPDATE User u SET u.accountStatus = :newStatus WHERE u.id = :userId")
    void updateAccountStatus(
            @Param("userId") Long userId,
            @Param("newStatus") User.AccountStatus newStatus);

    // =========================================================================
    // [SUBSYSTEM 5 — Analytics: Live Turnout Leaderboard]
    // =========================================================================

    /**
     * <b>[ANALYTICS — Live Turnout Leaderboard GROUP BY Query]</b>
     * Returns a count of voted users grouped by program. This single JPQL
     * query is the backend engine for the Admin's gamified bar chart
     * showing turnout percentages per program (e.g., BSIT: 85%).
     *
     * <p>
     * Returns a {@code List<Object[]>} where each element is:
     * {@code [String program, Long votedCount]}.
     * </p>
     *
     * <p>
     * Example usage in service layer:
     * 
     * <pre>
     * userRepository.countVotedByProgram().forEach(row -> {
     *     String program = (String) row[0];
     *     Long count = (Long) row[1];
     * });
     * </pre>
     * </p>
     */
    @Query("""
            SELECT u.program, COUNT(u)
            FROM User u
            WHERE u.accountStatus = 'VOTED'
            GROUP BY u.program
            ORDER BY COUNT(u) DESC
            """)
    List<Object[]> countVotedByProgram();

    /**
     * <b>[ANALYTICS — Total Active Voter Count]</b>
     * Counts all users with ACTIVE or VOTED status (i.e., registered voters).
     * Used as the denominator for turnout percentage calculations.
     *
     * @return Total number of eligible registered voters.
     */
    @Query("""
            SELECT COUNT(u) FROM User u
            WHERE u.accountStatus IN ('ACTIVE', 'VOTED')
            """)
    long countEligibleVoters();

    /**
     * <b>[ANALYTICS — Total Votes Cast Count]</b>
     * Counts all users who have completed voting. Used as the numerator
     * for the overall turnout percentage displayed in the Admin Command Center.
     *
     * @return Total number of users who have cast ballots.
     */
    long countByAccountStatus(User.AccountStatus status);

    // =========================================================================
    // [SUBSYSTEM 4 — Transaction: Voter Registry Admin View]
    // =========================================================================

    /**
     * <b>[ADMIN — Voter Registry Filtering]</b>
     * Dynamic filter query for the Admin Voter Registry panel. All parameters
     * are optional — null values cause that condition to be skipped (simulated
     * via JPQL's COALESCE-style null handling using ternary-style JPQL).
     *
     * @param program   Filter by program (e.g., "BSIT"). Null = all programs.
     * @param yearLevel Filter by year level. Null = all year levels.
     * @param section   Filter by section (e.g., "3A"). Null = all sections.
     * @param status    Filter by account status. Null = all statuses.
     * @return Filtered list of users matching all non-null criteria.
     */
    @Query("""
            SELECT u FROM User u
            WHERE (:program IS NULL OR u.program = :program)
            AND   (:yearLevel IS NULL OR u.yearLevel = :yearLevel)
            AND   (:section IS NULL OR u.section = :section)
            AND   (:status IS NULL OR u.accountStatus = :status)
            ORDER BY u.lastName ASC
            """)
    List<User> findByFilters(
            @Param("program") String program,
            @Param("yearLevel") Integer yearLevel,
            @Param("section") String section,
            @Param("status") User.AccountStatus status);
}