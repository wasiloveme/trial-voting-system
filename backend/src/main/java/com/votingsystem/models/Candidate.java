package com.votingsystem.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "candidates")
public class Candidate extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "position", nullable = false, length = 100)
    private String position;

    @Column(name = "party_list", length = 100)
    private String partylist;

    @Column(name = "platform", columnDefinition = "TEXT")
    private String platformText;

    @Column(name = "youtube_embed_url", length = 500)
    private String youtubeEmbedUrl;

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    @Builder.Default
    @Column(name = "vote_count", nullable = false)
    private Long voteCount = 0L;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 30)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING_APPROVAL;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getPartylist() { return partylist; }
    public void setPartylist(String partylist) { this.partylist = partylist; }
    public String getPlatformText() { return platformText; }
    public void setPlatformText(String platformText) { this.platformText = platformText; }
    public String getYoutubeEmbedUrl() { return youtubeEmbedUrl; }
    public void setYoutubeEmbedUrl(String youtubeEmbedUrl) { this.youtubeEmbedUrl = youtubeEmbedUrl; }
    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }
    public Long getVoteCount() { return voteCount; }
    public void setVoteCount(Long voteCount) { this.voteCount = voteCount; }
    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(ApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }

    public enum ApprovalStatus {
        PENDING_APPROVAL,
        APPROVED,
        REJECTED
    }
}
