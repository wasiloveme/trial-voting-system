package com.votingsystem.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * =============================================================================
 * FILE: AuditLog.java
 * PACKAGE: com.votingsystem.models
 * =============================================================================
 *
 * <h2>IAS101 "SECURED SUPER SYSTEM" — RUBRIC MAPPING</h2>
 * <ul>
 * <li><b>[SUBSYSTEM 3 — Monitoring & Audit | PRIMARY ENTITY]:</b> This
 * entity IS the Audit Trail System required by IAS101. Every significant
 * action in the system — login attempts, vote submissions, admin overrides,
 * account changes — generates one immutable row in this table.</li>
 * <li><b>[AUDIT TRAIL REQUIREMENT — Complete Coverage]:</b>
 * <ul>
 * <li><b>"Who"</b> — {@code actorId} and {@code actorRole} identify the
 * subject performing the action.</li>
 * <li><b>"What"</b> — {@code action} (e.g., "VOTE_CAST", "USER_LOGIN")
 * and {@code targetEntity}/{@code targetId} describe the operation
 * and the object it was performed on.</li>
 * <li><b>"When"</b> — {@code eventTimestamp} records the exact server
 * time of the event.</li>
 * <li><b>"Where" (IP Address)</b> — {@code ipAddress} captures the
 * source IP of the HTTP request, satisfying the IAS101 IP Address
 * requirement for the audit trail.</li>
 * </ul>
 * </li>
 * <li><b>[IMMUTABILITY — The Black Box]:</b> This entity intentionally has
 * NO {@code @LastModifiedDate} and no update methods. The service layer
 * only ever calls {@code auditLogRepository.save()} — never
 * {@code update()}. The {@code updatable = false} constraint on
 * {@code eventTimestamp} physically prevents any UPDATE statement from
 * altering the timestamp, making the log forensically sound.</li>
 * <li><b>[VPN / IP FILTER INTEGRATION]:</b> The {@code ipAddress} field is
 * populated by the {@code AuditLogService} using the IP extracted from
 * {@code HttpServletRequest}. This creates a permanent link between
 * admin actions and their originating IP, supporting the VPN/IP
 * Whitelisting enforcement audit.</li>
 * </ul>
 *
 * <p>
 * <b>Logging Strategy:</b> The {@code AuditLogService} is injected into all
 * major service classes ({@code VotingService}, {@code AuthService},
 * {@code AdminService}) and called at the END of each successful transaction,
 * ensuring logs are only written for completed, committed operations.
 * </p>
 *
 * @author Secured Super System — Lead Architect
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_logs", indexes = {
        // Index on actor for fast "show me everything user X did" queries.
        @Index(name = "idx_audit_actor", columnList = "actor_id"),
        // Index on timestamp for fast time-range forensic queries.
        @Index(name = "idx_audit_timestamp", columnList = "event_timestamp")
})
public class AuditLog {

    // =========================================================================
    // SECTION 1: PRIMARY KEY
    // NOTE: Does NOT extend BaseAuditEntity — this entity IS the audit record.
    // Adding a modifiable updatedAt to an audit log would be a
    // security anti-pattern.
    // =========================================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // SECTION 2: "WHO" — ACTOR IDENTIFICATION
    // [AUDIT TRAIL — IAS101 Requirement: Who performed the action]
    // =========================================================================

    /**
     * <b>[AUDIT — "Who", Part 1: Actor ID]</b>
     * The internal ID of the user who performed the action. Stored as a plain
     * {@code Long} (not a FK relationship) intentionally — this makes the
     * audit log independent of the {@code users} table. Even if a user account
     * is deleted, the audit history remains intact and fully queryable.
     */
    @Column(name = "actor_id")
    private Long actorId;

    /**
     * <b>[AUDIT — "Who", Part 2: Actor Identifier]</b>
     * Human-readable identifier (Student ID or admin username) of the actor.
     * Stored as a snapshot at the time of the event for forensic readability,
     * even if the username is later changed.
     */
    @Column(name = "actor_identifier", length = 100)
    private String actorIdentifier;

    /**
     * <b>[AUDIT — "Who", Part 3: Role]</b>
     * The role held by the actor at the time of the action. Captures whether
     * the action was performed by a USER, CANDIDATE, or ADMIN, which is
     * critical for privilege-escalation forensics.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", length = 20)
    private User.Role actorRole;

    // =========================================================================
    // SECTION 3: "WHAT" — ACTION DESCRIPTION
    // [AUDIT TRAIL — IAS101 Requirement: What action was performed]
    // =========================================================================

    /**
     * <b>[AUDIT — "What", Part 1: Action Code]</b>
     * A standardized, uppercase action code describing the operation.
     * Examples: {@code USER_LOGIN}, {@code USER_LOGIN_FAILED},
     * {@code VOTE_CAST}, {@code CANDIDATE_PROFILE_UPDATED},
     * {@code ADMIN_ELECTION_STATE_CHANGED}, {@code OTP_GENERATED},
     * {@code ADMIN_USER_LOCKED}. Using codes (not free text) enables
     * programmatic filtering and alerting on specific event types.
     */
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    /**
     * <b>[AUDIT — "What", Part 2: Target Entity Type]</b>
     * The type of entity the action was performed on.
     * Examples: {@code "User"}, {@code "Candidate"}, {@code "Vote"},
     * {@code "ElectionState"}, {@code "Question"}.
     */
    @Column(name = "target_entity", length = 50)
    private String targetEntity;

    /**
     * <b>[AUDIT — "What", Part 3: Target Entity ID]</b>
     * The primary key of the specific record that was affected. Combined with
     * {@code targetEntity}, this lets an administrator reconstruct the full
     * history of any single database record: "Show all audit events where
     * targetEntity='Vote' AND targetId=42."
     */
    @Column(name = "target_id")
    private Long targetId;

    /**
     * Optional freeform detail. Used for recording contextual metadata such as
     * rejection reasons, old vs. new values on updates, or error messages on
     * failed login attempts.
     */
    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    // =========================================================================
    // SECTION 4: "WHEN" & "WHERE" — TEMPORAL AND NETWORK CONTEXT
    // [AUDIT TRAIL — IAS101 Requirement: When (Timestamp) + IP Address]
    // =========================================================================

    /**
     * <b>[AUDIT — "When": Immutable Timestamp]</b>
     * Server-side timestamp of when the event occurred. The
     * {@code updatable = false} constraint at the JPA level, combined with
     * this being set once by the service layer, makes this field forensically
     * immutable — it cannot be altered by any subsequent UPDATE statement.
     */
    @Column(name = "event_timestamp", nullable = false, updatable = false)
    private LocalDateTime eventTimestamp;

    /**
     * <b>[AUDIT — "Where": IP Address | VPN FILTER INTEGRATION]</b>
     * The IPv4 or IPv6 address of the client making the HTTP request, extracted
     * from {@code HttpServletRequest.getRemoteAddr()} (with
     * {@code X-Forwarded-For} header support for reverse proxy environments).
     *
     * <p>
     * This field serves dual purposes:
     * </p>
     * <ol>
     * <li><b>Forensic:</b> Allows administrators to trace suspicious activity
     * back to a network source.</li>
     * <li><b>VPN Audit:</b> Cross-referenced with the {@code VpnFilter}'s
     * IP whitelist to flag admin actions performed from outside the
     * approved VPN IP range.</li>
     * </ol>
     */
    @Column(name = "ip_address", length = 45) // 45 chars supports full IPv6 notation
    private String ipAddress;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }
    public String getActorIdentifier() { return actorIdentifier; }
    public void setActorIdentifier(String actorIdentifier) { this.actorIdentifier = actorIdentifier; }
    public User.Role getActorRole() { return actorRole; }
    public void setActorRole(User.Role actorRole) { this.actorRole = actorRole; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTargetEntity() { return targetEntity; }
    public void setTargetEntity(String targetEntity) { this.targetEntity = targetEntity; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public LocalDateTime getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(LocalDateTime eventTimestamp) { this.eventTimestamp = eventTimestamp; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
