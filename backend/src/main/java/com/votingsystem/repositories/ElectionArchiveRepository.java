package com.votingsystem.repositories;

import com.votingsystem.models.ElectionArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * =============================================================================
 * FILE: ElectionArchiveRepository.java
 * PACKAGE: com.votingsystem.repositories
 * =============================================================================
 *
 * <h2>IAS101 "SECURED SUPER SYSTEM" — RUBRIC MAPPING</h2>
 * <ul>
 * <li><b>[SUBSYSTEM 5 — Analytics/Reporting: Historical Archives]:</b>
 * Read access for the Public Historical Archives view and the Admin
 * Archives Management dashboard. Write access used exclusively by the
 * Automated Archival Engine.</li>
 * </ul>
 *
 * @author Secured Super System — Lead Architect
 * @version 1.0
 */
@Repository
public interface ElectionArchiveRepository extends JpaRepository<ElectionArchive, Long> {

    /**
     * Returns all archived elections sorted newest-first for the historical
     * archives listing page.
     */
    List<ElectionArchive> findAllByOrderByArchivedAtDesc();

    /**
     * Checks if an archive already exists for a given academic year to prevent
     * duplicate archival runs from the cron job.
     *
     * @param electionYear The academic year string (e.g., "2024-2025").
     * @return {@code true} if an archive already exists for this year.
     */
    boolean existsByElectionYear(String electionYear);
}
