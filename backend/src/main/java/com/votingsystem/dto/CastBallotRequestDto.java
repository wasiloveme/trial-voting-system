package com.votingsystem.dto;
import lombok.Data;
import java.util.List;
@Data
public class CastBallotRequestDto { private List<Long> selectedCandidateIds; private List<String> abstainedPositions; }
