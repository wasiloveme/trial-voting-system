package com.votingsystem.repositories;

import com.votingsystem.models.Candidate;
import com.votingsystem.models.Candidate.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * =============================================================================
 * FILE: CandidateRepository.java
 * PACKAGE: com.votingsystem.repositories
 * =============================================================================
 *
 * <h2>IAS101 "SECURED SUPER SYSTEM" — RUBRIC MAPPING</h2>
 * <ul>
 * <li><b>[SUBSYSTEM 2 — Voting Core: Ballot Generation]:</b>
 * {@code findByApprovalStatus(APPROVED)} is the query that builds the
 * voter-facing ballot. Only Admin-approved candidates appear.</li>
 * <li><b>[SUBSYSTEM 3 — Monitoring: Blind Tallying]:</b>
 * {@code incrementVoteCount()} uses a direct UPDATE query rather than
 * a read-modify-write cycle, preventing race conditions when multiple
 * votes are processed concurrently.</li>
 * <li><b>[SUBSYSTEM 5 — Analytics: Archive Data Source]:</b>
 * {@code findAllWithVoteCounts()} is called by the Automated Archival
 * Engine at election close to snapshot final tallies.</li>
 * </ul>
 *
 * @author Secured Super System — Lead Architect
 * @version 1.0
 */
@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    // =========================================================================
    // [SUBSYSTEM 2 — Voting Core: Public Ballot Queries]
    // =========================================================================

    /**
     * <b>[BALLOT GENERATION — Public Voter Endpoint]</b>
     * Returns only Admin-approved candidates for the voter-facing dashboard.
     * The service layer maps these to {@code CandidatePublicDto} which
     * explicitly EXCLUDES the {@code voteCount} field (Blind Tallying).
     *
     * @param status Should always be {@code ApprovalStatus.APPROVED} when
     *               called from the public endpoint.
     * @return Approved candidates ordered by position for ballot layout.
     */
    List<Candidate> findByApprovalStatusOrderByPosition(ApprovalStatus status);

    /**
     * <b>[CANDIDATE FLOW — Profile Self-Lookup]</b>
     * Finds a candidate record linked to a specific user ID. Used when a
     * CANDIDATE-role user logs in and navigates to their Campaign Manager
     * — they can only see/edit their own profile.
     *
     * @param userId The User entity's primary key.
     * @return The candidate profile linked to this user, if it exists.
     */
    Optional<Candidate> findByUserId(Long userId);

    /**
     * <b>[ADMIN — Content Moderation Approval Queue]</b>
     * Returns all candidates with PENDING_APPROVAL status for the Admin's
     * review queue. Admins see these in the Ballot & Media approval panel.
     *
     * @return List of candidates awaiting admin review, oldest first.
     */
    List<Candidate> findByApprovalStatusOrderByCreatedAtAsc(ApprovalStatus status);

    // =========================================================================
    // [SUBSYSTEM 3 — Blind Tallying: Atomic Vote Count Increment]
    // =========================================================================

    /**
     * <b>[BLIND TALLYING — Race-Condition Safe Vote Increment]</b>
     * Uses a direct SQL UPDATE with {@code voteCount + 1} rather than a
     * read-modify-write pattern (read count → add 1 → save). This is critical
     * for correctness under concurrent load: if two votes arrive simultaneously,
     * a read-modify-write would read the same count value for both and write
     * the same incremented value — losing one vote. The SQL UPDATE is atomic
     * at the database transaction level, preventing this.
     *
     * @param candidateId The primary key of the candidate receiving the vote.
     */
    @Modifying
    @Query("UPDATE Candidate c SET c.voteCount = c.voteCount + 1 WHERE c.id = :candidateId")
    void incrementVoteCount(@Param("candidateId") Long candidateId);

    // =========================================================================
    // [SUBSYSTEM 5 — Analytics: Archival Data]
    // =========================================================================

    /**
     * <b>[AUTOMATED ARCHIVAL ENGINE — Final Tally Snapshot]</b>
     * Fetches all candidates with their vote counts at election close.
     * Called once by the archival cron job to build the
     * {@code winningCandidatesJson} snapshot for {@code ElectionArchive}.
     * Eagerly joins {@code user} to avoid N+1 when building the JSON.
     *
     * @return All candidates with their final vote totals, sorted by
     *         position then vote count descending (winners first per position).
     */
    @Query("""
            SELECT c FROM Candidate c
            JOIN FETCH c.user u
            WHERE c.approvalStatus = 'APPROVED'
            ORDER BY c.position ASC, c.voteCount DESC
            """)
    List<Candidate> findAllApprovedWithVoteCountsForArchive();
}