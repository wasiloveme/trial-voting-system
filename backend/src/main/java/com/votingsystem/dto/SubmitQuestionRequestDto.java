package com.votingsystem.dto;
import lombok.Data;
@Data
public class SubmitQuestionRequestDto { 
    private Long candidateId; 
    private String questionText; 

    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
}
