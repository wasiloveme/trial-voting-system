package com.votingsystem.services;

import com.votingsystem.dto.CandidateProfileUpdateDto;
import com.votingsystem.dto.CandidatePublicDto;
import com.votingsystem.models.Candidate;
import com.votingsystem.models.ElectionSettings;
import com.votingsystem.models.Question;
import com.votingsystem.repositories.CandidateRepository;
import com.votingsystem.repositories.ElectionSettingsRepository;
import com.votingsystem.repositories.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final ElectionSettingsRepository electionSettingsRepository;
    private final QuestionRepository questionRepository;
    private final AuditLogService auditLogService;
    private final InputSanitizationService sanitizationService;

    public CandidatePublicDto getCandidateProfileByStudentId(String studentId) {
        // Find candidate... assuming we have a way via User or custom query
        // This is a stub for now. The repository will need Candidate findByUserStudentId
        return new CandidatePublicDto(); 
    }

    public List<Question> getQuestionsForCandidate(String candidateIdStr) {
        // Stub implementation
        return List.of();
    }

    @Transactional
    public void updateCandidateProfile(String userIdStr, CandidateProfileUpdateDto platformDto, String clientIp) {
        Long userId = Long.parseLong(userIdStr);
        ElectionSettings settings = electionSettingsRepository.findFirstBy()
                .orElseThrow(() -> new RuntimeException("Election settings not configured"));

        if (settings.getElectionState() != ElectionSettings.ElectionState.PRE_ELECTION) {
            throw new RuntimeException("Platform updates are only allowed during PRE_ELECTION state.");
        }

        Candidate candidate = candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Candidate profile not found"));

        candidate.setPlatformText(sanitizationService.sanitizeText(platformDto.getPlatformText()));
        
        String cleanYoutubeUrl = sanitizationService.sanitizeUrl(platformDto.getYoutubeEmbedUrl());
        candidate.setYoutubeEmbedUrl(cleanYoutubeUrl);
        candidate.setApprovalStatus(Candidate.ApprovalStatus.PENDING_APPROVAL);

        candidateRepository.save(candidate);

        auditLogService.log(
                userId, "CANDIDATE", null,
                "CANDIDATE_PROFILE_UPDATED", "Candidate", candidate.getId(),
                "Candidate updated their platform and YouTube link", clientIp
        );
    }

    @Transactional
    public void submitAnswer(String candidateUserIdStr, Long questionId, String answerText, String clientIp) {
        Long candidateUserId = Long.parseLong(candidateUserIdStr);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        if (!question.getCandidate().getUser().getId().equals(candidateUserId)) {
            throw new RuntimeException("You are not authorized to answer this question.");
        }

        question.setAnswerText(sanitizationService.sanitizeText(answerText));
        question.setAnswerStatus(Question.AnswerStatus.PENDING_APPROVAL);

        questionRepository.save(question);

        auditLogService.log(
                candidateUserId, "CANDIDATE", null,
                "CANDIDATE_ANSWER_SUBMITTED", "Question", question.getId(),
                "Candidate submitted an answer to a question", clientIp
        );
    }
}
