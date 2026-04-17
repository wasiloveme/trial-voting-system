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

    public enum ApprovalStatus {
        PENDING_APPROVAL,
        APPROVED,
        REJECTED
    }
}
