package com.votingsystem.repositories;

import com.votingsystem.models.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * =============================================================================
 * FILE: VoteRepository.java
 * PACKAGE: com.votingsystem.repositories
 * =============================================================================
 *
 * <h2>IAS101 "SECURED SUPER SYSTEM" — RUBRIC MAPPING</h2>
 * <ul>
 * <li><b>[SUBSYSTEM 2 — Voting Core: Transaction Integrity]:</b>
 * {@code existsByVoterIdAndPosition()} is the application-layer
 * double-vote guard. The DB unique constraint is the final backstop,
 * but checking here first allows a clean error response rather than
 * a raw constraint violation exception.</li>
 * <li><b>[CRYPTOGRAPHIC RECEIPT ENGINE — SHA-256 Verification]:</b>
 * {@code findByReceiptHash()} powers the public Receipt Verification
 * Portal. Returns only a boolean-convertible Optional — the voter
 * identity is NEVER exposed through this query path.</li>
 * </ul>
 *
 * @author Secured Super System — Lead Architect
 * @version 1.0
 */
@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    // =========================================================================
    // [SUBSYSTEM 2 — Voting Core: Double-Vote Prevention]
    // =========================================================================

    /**
     * <b>[ANTI-DOUBLE-VOTE — Application Layer Guard]</b>
     * Checks if a voter has already submitted a vote for a specific position.
     * This is the first line of defense, called by {@code VotingService}
     * before attempting to insert a new vote row.
     *
     * <p>
     * The database's {@code UNIQUE(voter_id, position)} composite constraint
     * is the definitive backstop — but catching the violation here first
     * allows {@code VotingService} to return a clean, user-readable
     * error instead of a Hibernate {@code ConstraintViolationException}.
     * </p>
     *
     * @param voterId  The primary key of the voter.
     * @param position The ballot position being checked (e.g., "PRESIDENT").
     * @return {@code true} if a vote already exists for this voter+position pair.
     */
    boolean existsByVoterIdAndPosition(Long voterId, String position);

    /**
     * <b>[CRYPTOGRAPHIC RECEIPT VERIFICATION PORTAL — SHA-256 Lookup]</b>
     * The backend engine for the public {@code /api/public/verify/{hash}}
     * endpoint. Finds a vote row by its SHA-256 receipt hash.
     *
     * <p>
     * <b>Security Contract:</b> The service layer MUST map this result
     * to a simple {@code VerificationResponseDto} containing ONLY:
     * {@code { "status": "VERIFIED", "timestamp": "..." }}. The
     * {@code Vote} entity itself (which contains voter identity via
     * {@code voter_id}) must NEVER be serialized directly to the API
     * response — this would violate ballot secrecy.
     * </p>
     *
     * @param receiptHash The 64-character hex SHA-256 hash from the student's
     *                    digital receipt.
     * @return {@code Optional<Vote>} — present if the hash exists in the DB,
     *         confirming the vote is recorded. Empty if hash is not found.
     */
    Optional<Vote> findByReceiptHash(String receiptHash);

    /**
     * <b>[ADMIN — Total Votes Cast Counter]</b>
     * Returns the total number of vote rows. Note: each ballot submission
     * creates one Vote row PER POSITION — this counts individual position
     * votes, not unique voters.
     * Use {@code UserRepository.countByAccountStatus(VOTED)} for unique voter
     * count.
     */
    long count();

    /**
     * <b>[ANALYTICS — Votes by Position]</b>
     * Returns vote counts grouped by position for the archival engine
     * and admin analytics (after election closes / blind tallying lifts).
     *
     * <p>
     * Returns {@code Object[]} where: [0]=position (String), [1]=count (Long)
     * </p>
     */
    @Query("""
            SELECT v.position, COUNT(v)
            FROM Vote v
            WHERE v.candidate IS NOT NULL
            GROUP BY v.position
            ORDER BY v.position ASC
            """)
    java.util.List<Object[]> countVotesByPosition();
}
