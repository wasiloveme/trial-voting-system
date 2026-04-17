package com.votingsystem.repositories;

import com.votingsystem.models.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * =============================================================================
 * FILE: QuestionRepository.java
 * PACKAGE: com.votingsystem.repositories
 * =============================================================================
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
}
