package com.votingsystem.dto;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TurnoutLeaderboardDto { private String program; private Long turnoutCount; }
