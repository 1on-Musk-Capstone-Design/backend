package com.capstone.domain.idea.prototype;

import com.capstone.domain.idea.Idea;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "idea_prototype_jobs")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdeaPrototypeJob {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "idea_id", nullable = false)
  private Idea idea;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  @Setter
  private PrototypeJobStatus status;

  /** PRD 본문 (Markdown) */
  @Column(columnDefinition = "TEXT")
  @Setter
  private String prdMarkdown;

  /** UI 정보구조(JSON 문자열) */
  @Column(columnDefinition = "TEXT")
  @Setter
  private String uiStructureJson;

  /** 생성된 코드 파일 목록 요약(JSON, 경로→해시 등) */
  @Column(columnDefinition = "TEXT")
  @Setter
  private String generatedFilesSummaryJson;

  @Column(length = 512)
  @Setter
  private String githubRepoUrl;

  @Column(length = 512)
  @Setter
  private String vercelPreviewUrl;

  @Column(length = 512)
  @Setter
  private String vercelProductionUrl;

  /** 토큰 없이 시뮬레이션만 반환했는지 여부 */
  @Setter
  private Boolean simulated;

  /** Vercel REST API로 배포 생성에 성공했는지 여부 */
  @Setter
  private Boolean vercelDeploymentApiUsed;

  @Column(columnDefinition = "TEXT")
  @Setter
  private String errorMessage;

  @CreationTimestamp
  private Timestamp createdAt;

  @UpdateTimestamp
  private Timestamp updatedAt;
}
