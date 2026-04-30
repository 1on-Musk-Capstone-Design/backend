package com.capstone.domain.idea.prototype.dto;

import com.capstone.domain.idea.prototype.PrototypeJobStatus;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PrototypePipelineResponse {

  private final Long workspaceId;
  /** 팀이 같은 뷰를 열 때 사용(프론트 라우트, 상대 경로) */
  private final String prdViewPath;
  /** app.prd-public-base-url + prdViewPath (공유·복사용) */
  private final String prdViewUrl;
  private final Long jobId;
  private final Long ideaId;
  private final PrototypeJobStatus status;
  private final String prdMarkdown;
  private final String uiStructureJson;
  private final String githubRepoUrl;
  private final String vercelPreviewUrl;
  private final String vercelProductionUrl;
  private final Boolean simulated;
  /** Vercel 배포 API 성공 여부(null 가능) */
  private final Boolean vercelDeploymentApiUsed;
  private final String message;
  private final Instant createdAt;
  private final Instant updatedAt;
}
