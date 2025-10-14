package com.capstone.domain.canvas;

import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "canvas")
public class Canvas {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @Column(name = "workspace", nullable = false)
  private Workspace workspace;

  @ManyToOne
  @Column(name = "workspaceUser_id", nullable = false)
  private WorkspaceUser workspaceUser;

  @Column(name = "title", nullable = false)
  @Size(min = 1, message = "캔버스 제목은 최소 1자 이상이어야 합니다.")
  private String title;

  @CreationTimestamp
  private Timestamp createdAt;

  @UpdateTimestamp
  private Timestamp updatedAt;
}
