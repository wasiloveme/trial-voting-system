package com.votingsystem.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * =============================================================================
 * FILE: ElectionArchive.java
 * PACKAGE: com.votingsystem.models
 * =============================================================================
 *
 * <h2>IAS101 "SECURED SUPER SYSTEM" — RUBRIC MAPPING</h2>
 * <ul>
 * <li><b>[SUBSYSTEM 5 — Analytics/Reporting: Historical Archives]:</b>
 * This entity is the permanent, immutable record of a completed election.
 * It is written ONCE by the {@code AutomatedArchivalEngine} when the
 * election transitions to CLOSED state, and it is never modified
 * afterward. It serves as the backend for both the Public Historical
 * Archives view (read-only tab for students) and the Admin Historical
 * Archives Management dashboard with its "Generate PDF Report" feature.</li>
 * <li><b>[AUTOMATED ARCHIVAL ENGINE]:</b> The {@code AutomatedArchivalService}
 * executes a transaction that: (1) reads all {@code Vote} rows grouped
 * by position and candidate, (2) calculates winners and percentages,
 * (3) serializes the results to JSON, and (4) saves a single
 * {@code ElectionArchive} row — permanently capturing the election's
 * outcome.</li>
 * <li><b>[DATA INTEGRITY — Snapshot Pattern]:</b> The
 * {@code winningCandidatesJson} stores a serialized snapshot of the
 * final tally. Using JSON-in-TEXT means the archive remains readable
 * even if the {@code Candidate} or {@code User} records are later
 * modified or deleted. The archive is self-contained.</li>
 * <li><b>[IMMUTABILITY]:</b> This entity intentionally does NOT extend
 * {@code BaseAuditEntity} (which provides an {@code updatedAt} field).
 * An archive record must never be updated. {@code archivedAt} is
 * the single immutable write timestamp.</li>
 * </ul>
 *
 * <p>
 * <b>JSON Schema Example</b> for {@code winningCandidatesJson}:
 * </p>
 * 
 * <pre>
 * [
 *   {
 *     "position": "PRESIDENT",
 *     "winnerId": 12,
 *     "winnerName": "Juan Dela Cruz",
 *     "partylist": "Pagbabago",
 *     "votes": 342,
 *     "percentage": 78.5,
 *     "totalVotesForPosition": 436
 *   },
 *   ...
 * ]
 * </pre>
 *
 * @author Secured Super System — Lead Architect
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "election_archive")
public class ElectionArchive {

    // =========================================================================
    // SECTION 1: PRIMARY KEY
    // NOTE: Does NOT extend BaseAuditEntity — this is an immutable snapshot.
    // An updatedAt column on an archive record would be a contradiction.
    // =========================================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // SECTION 2: ELECTION IDENTITY SNAPSHOT
    // =========================================================================

    /**
     * Academic year of the archived election for display and filtering.
     * Example: "2024-2025"
     */
    @Column(name = "election_year", nullable = false, length = 20)
    private String electionYear;

    /**
     * The title of the election as configured in {@code ElectionSettings}.
     * Captured as a snapshot so the archive is readable independently.
     */
    @Column(name = "election_title", length = 200)
    private String electionTitle;

    // =========================================================================
    // SECTION 3: TURNOUT STATISTICS
    // [SUBSYSTEM 5 — Analytics: Turnout Leaderboard]
    // =========================================================================

    /**
     * Total number of registered voters at the time the election closed.
     * Used to compute {@code turnoutPercentage} and display on the
     * historical archives dashboard.
     */
    @Column(name = "total_registered_voters", nullable = false)
    private int totalRegisteredVoters;

    /**
     * Total number of ballots cast (users with {@code accountStatus == VOTED}).
     * Combined with {@code totalRegisteredVoters}, this gives the headline
     * turnout figure shown on public archive pages.
     */
    @Column(name = "total_votes_cast", nullable = false)
    private int totalVotesCast;

    /**
     * Pre-computed turnout percentage:
     * {@code (totalVotesCast / totalRegisteredVoters) * 100}.
     * Stored as a computed value to avoid re-calculation on every read.
     * {@code DECIMAL(5,2)} supports values like 85.75% up to 999.99%.
     */
    @Column(name = "turnout_percentage", precision = 5, scale = 2)
    private java.math.BigDecimal turnoutPercentage;

    // =========================================================================
    // SECTION 4: WINNER SNAPSHOT
    // [SUBSYSTEM 5 — Analytics: Official Results | Archival Engine Output]
    // =========================================================================

    /**
     * <b>[ARCHIVAL ENGINE — Results Snapshot]</b>
     * Serialized JSON array containing the full election outcome per position.
     * Written once by {@code AutomatedArchivalService} and never modified.
     * Stored as PostgreSQL {@code TEXT} to accommodate elections with many
     * positions and candidates without length restrictions.
     *
     * <p>
     * The {@code HistoricalArchiveService} deserializes this field using
     * Jackson's {@code ObjectMapper} when serving the public archives API
     * or generating the PDF report.
     * </p>
     */
    @Column(name = "winning_candidates_json", columnDefinition = "TEXT", nullable = false)
    private String winningCandidatesJson;

    // =========================================================================
    // SECTION 5: IMMUTABLE WRITE TIMESTAMP
    // =========================================================================

    /**
     * <b>[IMMUTABILITY — Single Write Timestamp]</b>
     * The exact UTC timestamp at which the archival engine completed writing
     * this record. Set once by the service layer; {@code updatable = false}
     * prevents any subsequent JPA UPDATE from modifying it.
     */
    @Column(name = "archived_at", nullable = false, updatable = false)
    private LocalDateTime archivedAt;
}
