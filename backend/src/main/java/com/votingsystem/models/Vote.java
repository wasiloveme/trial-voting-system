package com.votingsystem.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * =============================================================================
 * FILE: Vote.java
 * PACKAGE: com.votingsystem.models
 * =============================================================================
 *
 * <h2>IAS101 "SECURED SUPER SYSTEM" — RUBRIC MAPPING</h2>
 * <ul>
 * <li><b>[SUBSYSTEM 2 — Voting Core & Transaction Engine]:</b> This is the
 * most security-critical entity in the system. Each row represents one
 * immutable, transactional vote. The service layer wraps the entire
 * vote-casting operation in a single {@code @Transactional} block —
 * either the vote is recorded, the user status is flipped to VOTED,
 * and the receipt is generated atomically, or none of it happens.</li>
 * <li><b>[CRYPTOGRAPHIC RECEIPT ENGINE — SHA-256]:</b> The
 * {@code receiptHash} field stores the unique SHA-256 digest generated
 * at vote-cast time. It is the primary key of the public
 * Receipt Verification Portal — students can verify their vote was
 * counted without revealing WHO they voted for (preserving ballot
 * secrecy). This satisfies the Integrity pillar of the CIA triad.</li>
 * <li><b>[AUDIT TRAIL — "What" and "When"]:</b> The {@code castTimestamp}
 * provides an immutable server-side timestamp. Combined with the
 * inherited {@code BaseAuditEntity} fields, every vote has a
 * full temporal audit record.</li>
 * <li><b>[ABSTAIN SUPPORT]:</b> The {@code candidateId} field is nullable.
 * A {@code null} value with a populated {@code position} field
 * semantically represents a deliberate "Abstain" selection for that
 * position, distinguishing it from an uncast vote.</li>
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
@Table(name = "votes",
        // COMPOSITE UNIQUE CONSTRAINT: A voter can cast exactly one vote per
        // position. This is the database-level enforcement of the "one vote per
        // category" rule. Even if the application layer is bypassed, the DB will
        // reject a duplicate insert with a constraint violation exception.
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_voter_position", columnNames = { "voter_id", "position" })
        })
public class Vote extends BaseAuditEntity {

    // =========================================================================
    // SECTION 1: PRIMARY KEY
    // =========================================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // SECTION 2: VOTER REFERENCE
    // [SUBSYSTEM 2 — Voting Core | RBAC: Voter Identity Lock]
    // =========================================================================

    /**
     * <b>[TRANSACTION INTEGRITY — Voter Lock]</b>
     * Foreign key to the {@code users} table. After the vote is persisted,
     * the {@code VotingService} immediately updates the referenced user's
     * {@code accountStatus} to {@code VOTED} in the same transaction, making
     * it impossible for them to vote again without admin intervention.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id", nullable = false)
    private User voter;

    // =========================================================================
    // SECTION 3: BALLOT SELECTION
    // [SUBSYSTEM 2 — Voting Core | Abstain Support]
    // =========================================================================

    /**
     * The position this vote row covers (e.g., "PRESIDENT", "TREASURER").
     * Combined with {@code voter_id} in the unique constraint above to
     * enforce one-vote-per-position at the database level.
     */
    @Column(name = "position", nullable = false, length = 100)
    private String position;

    /**
     * <b>[ABSTAIN SUPPORT]</b>
     * The candidate the vote was cast for. This field is intentionally
     * {@code nullable}. When a student clicks "Abstain" for a position,
     * a Vote row IS still inserted (to prove the student engaged with that
     * position), but this FK is set to {@code null}, unambiguously recording
     * a deliberate abstention rather than a system error or skipped position.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = true)
    private Candidate candidate;

    // =========================================================================
    // SECTION 4: CRYPTOGRAPHIC RECEIPT
    // [CRYPTOGRAPHIC RECEIPT ENGINE — SHA-256 | CIA TRIAD: Integrity]
    // =========================================================================

    /**
     * <b>[IAS101 — CRYPTOGRAPHIC RECEIPT ENGINE | SHA-256 Hash]</b>
     * The unique SHA-256 digest generated by the {@code CryptoReceiptService}
     * at the exact moment of vote submission. The input to the hash function
     * is a concatenation of:
     * {@code voterId + position + candidateId + castTimestamp}.
     * This makes the hash unique, deterministic, and tamper-evident.
     *
     * <p>
     * <b>Verification Flow:</b> The student receives this hash on their
     * digital receipt. They can submit it to the public
     * {@code /api/public/verify/{hash}} endpoint. The backend runs a
     * {@code findByReceiptHash(hash)} query and returns only a boolean
     * "VERIFIED" or "NOT FOUND" — the voter's identity and selection are
     * NEVER exposed, preserving ballot secrecy.
     * </p>
     *
     * <p>
     * {@code unique = true} ensures no two votes can ever share the same
     * receipt hash, which would indicate a collision or data corruption
     * event.
     * </p>
     */
    @Column(name = "receipt_hash", nullable = false, unique = true, length = 64)
    private String receiptHash;

    /**
     * <b>[AUDIT — Immutable Vote Timestamp]</b>
     * Server-side timestamp recorded at the exact moment the vote transaction
     * commits. This is set by the {@code VotingService} and is displayed on
     * the student's digital receipt as proof of when their vote was cast.
     * This field is included in the SHA-256 hash input to bind the receipt
     * to a specific point in time, preventing pre-computation attacks.
     */
    @Column(name = "cast_timestamp", nullable = false, updatable = false)
    private LocalDateTime castTimestamp;

    @Column(name = "is_abstain", nullable = false)
    private boolean isAbstain = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getVoter() { return voter; }
    public void setVoter(User voter) { this.voter = voter; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public Candidate getCandidate() { return candidate; }
    public void setCandidate(Candidate candidate) { this.candidate = candidate; }
    public String getReceiptHash() { return receiptHash; }
    public void setReceiptHash(String receiptHash) { this.receiptHash = receiptHash; }
    public LocalDateTime getCastTimestamp() { return castTimestamp; }
    public void setCastTimestamp(LocalDateTime castTimestamp) { this.castTimestamp = castTimestamp; }
    public boolean getIsAbstain() { return isAbstain; }
    public void setIsAbstain(boolean isAbstain) { this.isAbstain = isAbstain; }
}
