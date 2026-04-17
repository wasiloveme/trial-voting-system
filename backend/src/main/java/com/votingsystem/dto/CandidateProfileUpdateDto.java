package com.votingsystem.dto;
import lombok.Data;
@Data
public class CandidateProfileUpdateDto { 
    private String platformText; 
    private String youtubeEmbedUrl; 

    public String getPlatformText() { return platformText; }
    public void setPlatformText(String platformText) { this.platformText = platformText; }
    public String getYoutubeEmbedUrl() { return youtubeEmbedUrl; }
    public void setYoutubeEmbedUrl(String youtubeEmbedUrl) { this.youtubeEmbedUrl = youtubeEmbedUrl; }
}
