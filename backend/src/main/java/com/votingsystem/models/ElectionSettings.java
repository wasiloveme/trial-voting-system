package com.votingsystem.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "election_settings")
public class ElectionSettings extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "election_state", nullable = false, length = 20)
    private ElectionState electionState = ElectionState.PRE_ELECTION;

    @Column(name = "countdown_end_time")
    private LocalDateTime countdownEndTime;

    @Column(name = "election_title", length = 200)
    private String electionTitle;

    @Column(name = "academic_year", length = 20)
    private String academicYear;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ElectionState getElectionState() { return electionState; }
    public void setElectionState(ElectionState electionState) { this.electionState = electionState; }
    public java.time.LocalDateTime getCountdownEndTime() { return countdownEndTime; }
    public void setCountdownEndTime(java.time.LocalDateTime countdownEndTime) { this.countdownEndTime = countdownEndTime; }
    public String getElectionTitle() { return electionTitle; }
    public void setElectionTitle(String electionTitle) { this.electionTitle = electionTitle; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public enum ElectionState {
        PRE_ELECTION,
        OPEN,
        PAUSED,
        CLOSED
    }
}
