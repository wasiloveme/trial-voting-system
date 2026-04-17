package com.votingsystem.repositories;

import com.votingsystem.models.ElectionSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * =============================================================================
 * FILE: ElectionSettingsRepository.java
 * PACKAGE: com.votingsystem.repositories
 * =============================================================================
 *
 * <h2>IAS101 "SECURED SUPER SYSTEM" — RUBRIC MAPPING</h2>
 * <ul>
 * <li><b>[SUBSYSTEM 2 — Voting Core: Admin Master Switch]:</b>
 * This repository reads and writes the singleton election state row.
 * Every service that needs to check whether voting is OPEN, PAUSED,
 * or CLOSED calls {@code findFirstBy()} through the
 * {@code ElectionSettingsService}.</li>
 * </ul>
 *
 * @author Secured Super System — Lead Architect
 * @version 1.0
 */
@Repository
public interface ElectionSettingsRepository extends JpaRepository<ElectionSettings, Long> {

    /**
     * <b>[SINGLETON ROW ACCESS]</b>
     * Fetches the single active election configuration row.
     * Because the table is designed to hold exactly one row (enforced by
     * the partial unique index in {@code schema.sql}), {@code findFirstBy()}
     * is the safe, idempotent way to retrieve it without knowing the row ID.
     *
     * @return The current election settings, wrapped in Optional.
     *         Should always be present after DB initialization via schema.sql seed.
     */
    Optional<ElectionSettings> findFirstBy();
}