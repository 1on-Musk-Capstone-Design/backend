package com.capstone.domain.workspaceUser;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.workspace.Workspace;
import com.capstone.global.type.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "workspace_users")
public class WorkspaceUser {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "workspace_id", nullable = false)
  private Workspace workspace;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  private Role role;

  @CreationTimestamp
  @Column(name = "joined_at", nullable = false, updatable = false)
  private Timestamp joinedAt;

  public void updateRole(Role role) {
    this.role = role;
  }
}
