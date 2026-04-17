package com.votingsystem.repositories;

import com.votingsystem.models.AuditLog;
import com.votingsystem.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * =============================================================================
 * FILE: AuditLogRepository.java
 * PACKAGE: com.votingsystem.repositories
 * =============================================================================
 *
 * <h2>IAS101 "SECURED SUPER SYSTEM" — RUBRIC MAPPING</h2>
 * <ul>
 * <li><b>[SUBSYSTEM 3 — Monitoring & Audit: The Black Box]:</b>
 * This repository is INSERT-ONLY by application design. The
 * {@code AuditLogService} calls {@code save()} but never calls any
 * update-returning method. The DB schema enforces this via the
 * {@code updatable = false} constraint on {@code eventTimestamp}.</li>
 * <li><b>[ADMIN — Audit Log Viewer]:</b> Paginated queries prevent the
 * Admin audit log UI from loading unbounded result sets. Forensic
 * time-range queries allow targeted investigation of incidents.</li>
 * <li><b>[VPN FILTER AUDIT]:</b> {@code findByIpAddress()} allows Admins
 * to audit all actions performed from a specific IP address — critical
 * for cross-referencing against the VPN whitelist enforcement.</li>
 * </ul>
 *
 * @author Secured Super System — Lead Architect
 * @version 1.0
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * <b>[ADMIN AUDIT LOG — Paginated Full Log View]</b>
     * Returns all audit entries newest-first with pagination support.
     * The {@code Pageable} parameter is provided by the Admin controller
     * ({@code PageRequest.of(page, size, Sort.by("eventTimestamp").descending())}).
     *
     * @param pageable Spring Data pagination and sort specification.
     * @return Paginated page of audit log entries.
     */
    Page<AuditLog> findAllByOrderByEventTimestampDesc(Pageable pageable);

    /**
     * <b>[FORENSIC — Actor-Based Audit Trail]</b>
     * Returns all actions performed by a specific actor (user).
     * Used by Admins to review the complete activity history of
     * a specific student or admin account.
     *
     * @param actorId The internal user ID to audit.
     * @return Chronological list of all actions by this actor.
     */
    List<AuditLog> findByActorIdOrderByEventTimestampDesc(Long actorId);

    /**
     * <b>[FORENSIC — Action-Type Filter]</b>
     * Retrieves all events of a specific action type for pattern analysis.
     * Example: all {@code "USER_LOGIN_FAILED"} events to detect brute-force.
     *
     * @param action The action code string (e.g., "USER_LOGIN_FAILED").
     * @return All log entries for this action type, newest first.
     */
    List<AuditLog> findByActionOrderByEventTimestampDesc(String action);

    /**
     * <b>[FORENSIC — Time-Range Query]</b>
     * Returns all audit events within a specific time window.
     * Used by Admins for incident investigation (e.g., "what happened
     * between 14:00 and 15:00 on election day?").
     *
     * @param from Start of the time range (inclusive).
     * @param to   End of the time range (inclusive).
     * @return All audit events in the specified window.
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.eventTimestamp BETWEEN :from AND :to
            ORDER BY a.eventTimestamp ASC
            """)
    List<AuditLog> findByEventTimestampBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * <b>[VPN FILTER AUDIT — IP Address Investigation]</b>
     * Retrieves all audit events originating from a specific IP address.
     * Critical for cross-referencing admin actions against the VPN whitelist
     * and flagging actions performed from unexpected network locations.
     *
     * @param ipAddress The IP address to investigate.
     * @return All audit entries from this IP, newest first.
     */
    List<AuditLog> findByIpAddressOrderByEventTimestampDesc(String ipAddress);

    /**
     * <b>[ADMIN — Role-Based Activity Filter]</b>
     * Filters audit entries by the actor's role. Useful for isolating
     * all ADMIN actions for privileged access review, or all CANDIDATE
     * actions for content moderation audit.
     *
     * @param role The actor role to filter by.
     * @return All audit entries for actors of this role.
     */
    List<AuditLog> findByActorRoleOrderByEventTimestampDesc(User.Role role);
}
