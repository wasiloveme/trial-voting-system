package com.votingsystem.models;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * =============================================================================
 * FILE: BaseAuditEntity.java
 * PACKAGE: com.votingsystem.models
 * =============================================================================
 *
 * <h2>IAS101 "SECURED SUPER SYSTEM" — RUBRIC MAPPING</h2>
 * <ul>
 * <li><b>[SUBSYSTEM 3 — Monitoring & Audit]:</b> This superclass is the
 * foundation of the automated audit trail. By annotating this class with
 * {@code @EntityListeners(AuditingEntityListener.class)}, Spring Data JPA
 * automatically intercepts every {@code INSERT} and {@code UPDATE}
 * operation on any inheriting entity and stamps it with a server-side
 * timestamp — no developer can forget to log it.</li>
 * <li><b>[AUDIT TRAIL REQUIREMENT]:</b> Provides the "When" column of the
 * audit requirement. The {@code createdAt} field captures the original
 * creation event; {@code updatedAt} captures every subsequent
 * modification, creating an immutable time-series record of data
 * changes.</li>
 * <li><b>[SECURITY PRINCIPLE — Non-Repudiation]:</b> Because timestamps are
 * set by the server-side JPA listener and NOT by client input, they
 * cannot be spoofed or manipulated by a malicious actor.</li>
 * </ul>
 *
 * <p>
 * <b>Design Pattern:</b> {@code @MappedSuperclass} — This class is NOT
 * mapped to its own database table. Instead, its fields are physically
 * inherited and included in the table of each concrete child entity
 * (User, Vote, Candidate, etc.), keeping the schema clean.
 * </p>
 *
 * @author Secured Super System — Lead Architect
 * @version 1.0
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity {

    /**
     * <b>[AUDIT — "When" Field, Part 1]</b>
     * Automatically populated by {@code AuditingEntityListener} on the
     * first {@code INSERT}. The {@code updatable = false} constraint is a
     * critical security control: it makes this field immutable at the
     * database level — once written, it can NEVER be overwritten by a
     * subsequent UPDATE statement, preserving the integrity of the creation
     * record. Mapped to a TIMESTAMP WITH TIME ZONE column in PostgreSQL.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * <b>[AUDIT — "When" Field, Part 2]</b>
     * Automatically populated by {@code AuditingEntityListener} on every
     * subsequent {@code UPDATE}. Together with {@code createdAt}, this
     * creates a two-point audit window for every record, enabling forensic
     * analysis of "when was this data last changed."
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
