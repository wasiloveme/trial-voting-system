package com.votingsystem.services;

import com.votingsystem.models.Candidate;
import com.votingsystem.models.ElectionSettings;
import com.votingsystem.models.Question;
import com.votingsystem.models.User;
import com.votingsystem.repositories.CandidateRepository;
import com.votingsystem.repositories.ElectionSettingsRepository;
import com.votingsystem.repositories.QuestionRepository;
import com.votingsystem.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ElectionSettingsRepository electionSettingsRepository;
    private final CandidateRepository candidateRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public void changeElectionState(String adminIdStr, ElectionSettings.ElectionState state, LocalDateTime countdownEndTime, String clientIp) {
        Long adminId = Long.parseLong(adminIdStr);
        ElectionSettings settings = electionSettingsRepository.findFirstBy()
                .orElse(new ElectionSettings());

        ElectionSettings.ElectionState oldState = settings.getElectionState();
        settings.setElectionState(state);
        settings.setCountdownEndTime(countdownEndTime);
        
        electionSettingsRepository.save(settings);

        auditLogService.log(
                adminId, "ADMIN", null,
                "ELECTION_STATE_CHANGED", "ElectionSettings", settings.getId(),
                "State changed from " + oldState + " to " + state, clientIp
        );
    }

    public ElectionSettings getCurrentElectionSettings() {
        return electionSettingsRepository.findFirstBy().orElse(new ElectionSettings());
    }

    public List<Candidate> getCandidateVoteCounts() {
        return candidateRepository.findAll();
    }

    public List<Candidate> getPendingApprovalCandidates() {
        return candidateRepository.findAll().stream()
                .filter(c -> c.getApprovalStatus() == Candidate.ApprovalStatus.PENDING_APPROVAL)
                .toList();
    }

    @Transactional
    public void setCandidateApprovalStatus(String adminIdStr, Long candidateId, Candidate.ApprovalStatus status, String clientIp) {
        Long adminId = Long.parseLong(adminIdStr);
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));

        candidate.setApprovalStatus(status);
        candidateRepository.save(candidate);

        auditLogService.log(
                adminId, "ADMIN", null,
                "CANDIDATE_PLATFORM_REVIEWED", "Candidate", candidate.getId(),
                "Platform review: " + status, clientIp
        );
    }

    @Transactional
    public void setAnswerApprovalStatus(String adminIdStr, Long questionId, Candidate.ApprovalStatus status, String clientIp) {
        Long adminId = Long.parseLong(adminIdStr);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        // Match Candidate.ApprovalStatus to Question.AnswerStatus conceptually
        if (status == Candidate.ApprovalStatus.APPROVED) {
            question.setAnswerStatus(Question.AnswerStatus.APPROVED);
        } else if (status == Candidate.ApprovalStatus.REJECTED) {
            question.setAnswerStatus(Question.AnswerStatus.REJECTED);
        }

        questionRepository.save(question);

        auditLogService.log(
                adminId, "ADMIN", null,
                "QUESTION_ANSWER_REVIEWED", "Question", question.getId(),
                "Answer review: " + status, clientIp
        );
    }

    public List<User> getUsersByFilters(String search, Integer yearLevel, String program, User.AccountStatus status) {
        return userRepository.findAll(); // Stub implementation
    }

    @Transactional
    public void setUserAccountStatus(String adminIdStr, Long userId, User.AccountStatus status, String clientIp) {
        Long adminId = Long.parseLong(adminIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setAccountStatus(status);
        userRepository.save(user);

        auditLogService.log(
                adminId, "ADMIN", null,
                "USER_STATUS_CHANGED", "User", user.getId(),
                "Status changed to: " + status, clientIp
        );
    }

    public Object getAuditLog(PageRequest pageRequest) {
        // Return Audit logs. Stub for now
        return null;
    }

    public Object getAuditLogByIp(String ip) {
        return null;
    }

    public Object getAllArchives() {
        return null;
    }

    public Object generateElectionPdfReport(Long archiveId, String adminIdStr, String clientIp) {
        return null;
    }

    public ElectionSettings.ElectionState getPublicElectionState() {
        return getCurrentElectionSettings().getElectionState();
    }
}