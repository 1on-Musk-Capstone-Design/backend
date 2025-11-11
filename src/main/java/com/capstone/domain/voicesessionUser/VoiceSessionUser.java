package com.capstone.domain.voicesessionUser;

import com.capstone.domain.voicesession.VoiceSession;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "voice_session_users")
public class VoiceSessionUser {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "session_id", nullable = false)
  private VoiceSession session;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "workspace_user_id", nullable = false)
  private WorkspaceUser workspaceUser;

  @CreationTimestamp
  @Column(name = "joined_at", nullable = false, updatable = false)
  private LocalDateTime joinedAt;

  @Column(name = "left_at")
  private LocalDateTime leftAt;

  // 비즈니스 메서드: 퇴장
  public void leave() {
    if (this.leftAt != null) {
      throw new IllegalStateException("이미 세션에서 퇴장했습니다.");
    }
    this.leftAt = LocalDateTime.now();
  }

  // 비즈니스 메서드: 현재 참여 중인지 확인
  public boolean isActive() {
    return this.leftAt == null;
  }
}