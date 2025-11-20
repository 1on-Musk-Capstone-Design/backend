package com.capstone.domain.workspaceInvitation;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.workspace.Workspace;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "workspace_invitations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceInvitation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "workspace_id", nullable = false)
  private Workspace workspace;

  @ManyToOne
  @JoinColumn(name = "invited_user_id", nullable = false)
  private User invitedUser;

  @ManyToOne
  @JoinColumn(name = "invited_by_user_id", nullable = false)
  private User invitedBy;

  @Column(name = "status", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private InvitationStatus status = InvitationStatus.PENDING;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "expires_at")
  private Instant expiresAt;

  public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    REJECTED
  }
}

