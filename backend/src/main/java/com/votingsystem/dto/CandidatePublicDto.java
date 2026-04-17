package com.votingsystem.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidatePublicDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String program;
    private String position;
    private String partylist;
    private String platformText;
    private String youtubeEmbedUrl;
    private String profilePictureUrl;
}
