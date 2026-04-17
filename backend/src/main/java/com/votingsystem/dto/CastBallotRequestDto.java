package com.votingsystem.dto;
import lombok.Data;
import java.util.List;
@Data
public class CastBallotRequestDto { 
    private List<Long> selectedCandidateIds; 
    private List<String> abstainedPositions; 

    public List<Long> getSelectedCandidateIds() { return selectedCandidateIds; }
    public void setSelectedCandidateIds(List<Long> selectedCandidateIds) { this.selectedCandidateIds = selectedCandidateIds; }
    public List<String> getAbstainedPositions() { return abstainedPositions; }
    public void setAbstainedPositions(List<String> abstainedPositions) { this.abstainedPositions = abstainedPositions; }
}
