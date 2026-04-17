package com.votingsystem.services;

import com.votingsystem.dto.*;
import com.votingsystem.models.User;
import com.votingsystem.models.Vote;
import com.votingsystem.models.Candidate;
import com.votingsystem.models.ElectionSettings;
import com.votingsystem.models.Question;
import com.votingsystem.repositories.CandidateRepository;
import com.votingsystem.repositories.ElectionSettingsRepository;
import com.votingsystem.repositories.QuestionRepository;
import com.votingsystem.repositories.UserRepository;
import com.votingsystem.repositories.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VotingService {

    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final ElectionSettingsRepository electionSettingsRepository;
    private final QuestionRepository questionRepository;
    private final AuditLogService auditLogService;
    private final InputSanitizationService sanitizationService;

    public CandidatePublicDto getCandidateProfile(Long candidateId) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));
        // Manual mapping for stub
        CandidatePublicDto dto = new CandidatePublicDto();
        return dto;
    }

    public java.util.List<CandidatePublicDto> getCandidatePublicProfiles() {
        // Stub
        return java.util.List.of();
    }

    public ReceiptVerificationDto verifyReceipt(String receiptHash) {
        // Stub
        ReceiptVerificationDto dto = new ReceiptVerificationDto();
        dto.setReceiptHash(receiptHash);
        return dto;
    }

    public String getVoterStatus(String voterIdStr) {
        Long voterId = Long.parseLong(voterIdStr);
        User user = userRepository.findById(voterId)
                .orElseThrow(() -> new RuntimeException("Voter not found"));
        return user.getAccountStatus().name();
    }

    @Transactional
    public VoteReceiptDto castBallot(String voterIdStr, CastBallotRequestDto ballotDto, String clientIp) {
        Long voterId = Long.parseLong(voterIdStr);
        ElectionSettings settings = electionSettingsRepository.findFirstBy()
                .orElseThrow(() -> new RuntimeException("Election settings not configured"));

        if (settings.getElectionState() != ElectionSettings.ElectionState.OPEN) {
            throw new RuntimeException("Voting is currently closed.");
        }

        User voter = userRepository.findById(voterId)
                .orElseThrow(() -> new RuntimeException("Voter not found"));

        if (voter.getAccountStatus() == User.AccountStatus.VOTED) {
            throw new RuntimeException("You have already cast your ballot.");
        }

        String rawReceiptData = voter.getStudentId() + "-" + UUID.randomUUID().toString() + "-" + LocalDateTime.now().toString();
        String receiptHash = generateSha256Hash(rawReceiptData);

        for (Long candidateId : ballotDto.getSelectedCandidateIds()) {
            Candidate candidate = candidateRepository.findById(candidateId)
                    .orElseThrow(() -> new RuntimeException("Candidate not found: " + candidateId));
            
            Vote vote = new Vote();
            vote.setVoter(voter);
            vote.setCandidate(candidate);
            vote.setPosition(candidate.getPosition());
            vote.setReceiptHash(receiptHash);
            vote.setIsAbstain(false);
            voteRepository.save(vote);
            
            candidate.setVoteCount(candidate.getVoteCount() + 1);
            candidateRepository.save(candidate);
        }

        if (ballotDto.getAbstainedPositions() != null) {
            for (String position : ballotDto.getAbstainedPositions()) {
                Vote abstainVote = new Vote();
                abstainVote.setVoter(voter);
                abstainVote.setPosition(position);
                abstainVote.setIsAbstain(true);
                abstainVote.setReceiptHash(receiptHash);
                voteRepository.save(abstainVote);
            }
        }

        voter.setAccountStatus(User.AccountStatus.VOTED);
        userRepository.save(voter);

        auditLogService.log(
                voter.getId(), voter.getStudentId(), voter.getRole(),
                "BALLOT_CAST", "Vote", null,
                "Voter cast their ballot successfully", clientIp
        );

        VoteReceiptDto receiptDto = new VoteReceiptDto();
        receiptDto.setReceiptHash(receiptHash);
        return receiptDto;
    }

    @Transactional
    public void submitQuestion(String voterIdStr, SubmitQuestionRequestDto dto, String clientIp) {
        Long voterId = Long.parseLong(voterIdStr);
        User voter = userRepository.findById(voterId)
                .orElseThrow(() -> new RuntimeException("Voter not found"));

        Candidate candidate = candidateRepository.findById(dto.getCandidateId())
                .orElseThrow(() -> new RuntimeException("Candidate not found"));

        Question question = new Question();
        question.setVoter(voter);
        question.setCandidate(candidate);
        question.setQuestionText(sanitizationService.sanitizeText(dto.getQuestionText()));
        question.setAnswerStatus(Question.AnswerStatus.UNANSWERED);

        questionRepository.save(question);

        auditLogService.log(
                voter.getId(), voter.getStudentId(), voter.getRole(),
                "QUESTION_SUBMITTED", "Question", question.getId(),
                "Voter submitted a question to candidate", clientIp
        );
    }

    private String generateSha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate digital receipt", e);
        }
    }
}
