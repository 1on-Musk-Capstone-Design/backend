package com.capstone.domain.idea.prototype;

import static com.capstone.global.exception.ErrorCode.FORBIDDEN_WORKSPACE_ACCESS;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_IDEA;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_PROTOTYPE_JOB;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_PROTOTYPE_JOB_BY_ID;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_USER;
import static com.capstone.global.exception.ErrorCode.PROTOTYPE_PIPELINE_FAILED;

import com.capstone.domain.idea.Idea;
import com.capstone.domain.idea.IdeaRepository;
import com.capstone.domain.idea.prototype.dto.PrototypeJobAcceptedResponse;
import com.capstone.domain.idea.prototype.dto.PrototypeJobSummaryResponse;
import com.capstone.domain.idea.prototype.dto.PrototypePipelineResponse;
import com.capstone.domain.idea.prototype.dto.PrototypeSourceFileResponse;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.user.UserRepository;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import com.capstone.global.exception.CustomException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdeaPrototypeService {

  @Value("${app.prd-public-base-url:http://localhost:3000}")
  private String prdPublicBaseUrl;
  @Value("${app.local-dev-permit-all-workspace-access:false}")
  private boolean localDevPermitAllWorkspaceAccess;

  private final IdeaRepository ideaRepository;
  private final UserRepository userRepository;
  private final WorkspaceUserRepository workspaceUserRepository;
  private final IdeaPrototypeJobRepository jobRepository;
  private final PrototypePipelineExecutionService prototypePipelineExecutionService;
  private final PrototypePipelineRunner prototypePipelineRunner;
  private final PrototypeArtifactService prototypeArtifactService;

  @Transactional
  public PrototypeJobAcceptedResponse startPipelineAsync(Long userId, Long ideaId) {
    Idea idea = loadIdeaAndCheckAccess(userId, ideaId);
    // 동일 아이디어에 대기 중이던 이전 작업은 새 PRD로 대체되므로 취소 (중복 파이프라인·500 완화)
    for (IdeaPrototypeJob j : jobRepository.findByIdea_IdOrderByIdDesc(idea.getId())) {
      if (j.getStatus() == PrototypeJobStatus.PENDING) {
        j.setStatus(PrototypeJobStatus.FAILED);
        j.setErrorMessage("새 PRD 파이프라인이 시작되어 이 대기 작업은 취소되었습니다.");
        jobRepository.save(j);
      }
    }
    IdeaPrototypeJob job =
        jobRepository.save(
            IdeaPrototypeJob.builder()
                .idea(idea)
                .status(PrototypeJobStatus.PENDING)
                .simulated(false)
                .build());
    Long jobIdToRun = job.getId();
    // 트랜잭션 커밋 후 비동기 실행 — 커밋 전에 워커가 조회하면 job 행이 없어 파이프라인이 실패·PENDING 고착 가능
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              prototypePipelineRunner.runPipelineAsync(jobIdToRun);
            }
          });
    } else {
      prototypePipelineRunner.runPipelineAsync(jobIdToRun);
    }
    return PrototypeJobAcceptedResponse.builder()
        .prdId(job.getId())
        .jobId(job.getId())
        .ideaId(ideaId)
        .status(PrototypeJobStatus.PENDING)
        .message("파이프라인이 백그라운드에서 실행됩니다. GET /prototype/jobs/{jobId}로 상태를 확인하세요.")
        .build();
  }

  @Transactional
  public PrototypePipelineResponse runPipelineSync(Long userId, Long ideaId) {
    Idea idea = loadIdeaAndCheckAccess(userId, ideaId);
    for (IdeaPrototypeJob j : jobRepository.findByIdea_IdOrderByIdDesc(idea.getId())) {
      if (j.getStatus() == PrototypeJobStatus.PENDING) {
        j.setStatus(PrototypeJobStatus.FAILED);
        j.setErrorMessage("새 PRD 파이프라인이 시작되어 이 대기 작업은 취소되었습니다.");
        jobRepository.save(j);
      }
    }
    IdeaPrototypeJob job =
        jobRepository.save(
            IdeaPrototypeJob.builder()
                .idea(idea)
                .status(PrototypeJobStatus.PENDING)
                .simulated(false)
                .build());
    prototypePipelineExecutionService.executePipeline(job.getId());
    IdeaPrototypeJob updated =
        jobRepository.findById(job.getId()).orElseThrow(() -> new CustomException(NOT_FOUND_PROTOTYPE_JOB));
    if (updated.getStatus() == PrototypeJobStatus.FAILED) {
      throw new CustomException(PROTOTYPE_PIPELINE_FAILED);
    }
    return toResponse(updated, idea, ideaId, false);
  }

  @Transactional
  public PrototypeJobAcceptedResponse retryPipelineAsync(Long userId, Long ideaId) {
    return startPipelineAsync(userId, ideaId);
  }

  @Transactional(readOnly = true)
  public PrototypePipelineResponse getLatest(Long userId, Long ideaId) {
    Idea idea = loadIdeaAndCheckAccess(userId, ideaId);
    IdeaPrototypeJob job =
        jobRepository
            .findTopByIdea_IdOrderByIdDesc(idea.getId())
            .orElseThrow(() -> new CustomException(NOT_FOUND_PROTOTYPE_JOB));
    return toResponse(job, idea, ideaId, false);
  }

  @Transactional(readOnly = true)
  public PrototypePipelineResponse getJob(Long userId, Long ideaId, Long jobId) {
    Idea idea = loadIdeaAndCheckAccess(userId, ideaId);
    IdeaPrototypeJob job =
        jobRepository
            .findByIdea_IdAndId(idea.getId(), jobId)
            .orElseThrow(() -> new CustomException(NOT_FOUND_PROTOTYPE_JOB_BY_ID));
    return toResponse(job, idea, ideaId, false);
  }

  @Transactional(readOnly = true)
  public List<PrototypeSourceFileResponse> listSourceFiles(
      Long userId, Long ideaId, Long jobId) {
    Idea idea = loadIdeaAndCheckAccess(userId, ideaId);
    jobRepository
        .findByIdea_IdAndId(idea.getId(), jobId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_PROTOTYPE_JOB_BY_ID));
    return prototypeArtifactService.listForJob(jobId);
  }

  @Transactional(readOnly = true)
  public PrototypePipelineResponse getWorkspacePrd(Long userId, Long workspaceId, Long prdId) {
    IdeaPrototypeJob job =
        jobRepository
            .findById(prdId)
            .orElseThrow(() -> new CustomException(NOT_FOUND_PROTOTYPE_JOB_BY_ID));
    Idea idea = requireWorkspaceScopedIdea(job, workspaceId);
    ensureWorkspaceAccess(userId, idea.getWorkspace());
    return toResponse(job, idea, idea.getId(), true);
  }

  @Transactional(readOnly = true)
  public List<PrototypeSourceFileResponse> listWorkspacePrdSourceFiles(
      Long userId, Long workspaceId, Long prdId) {
    IdeaPrototypeJob job =
        jobRepository
            .findById(prdId)
            .orElseThrow(() -> new CustomException(NOT_FOUND_PROTOTYPE_JOB_BY_ID));
    Idea idea = requireWorkspaceScopedIdea(job, workspaceId);
    ensureWorkspaceAccess(userId, idea.getWorkspace());
    return prototypeArtifactService.listForJob(prdId);
  }

  @Transactional(readOnly = true)
  public String getWorkspacePrdPreviewHtml(Long userId, Long workspaceId, Long prdId) {
    IdeaPrototypeJob job =
        jobRepository
            .findById(prdId)
            .orElseThrow(() -> new CustomException(NOT_FOUND_PROTOTYPE_JOB_BY_ID));
    Idea idea = requireWorkspaceScopedIdea(job, workspaceId);
    ensureWorkspaceAccess(userId, idea.getWorkspace());
    try {
      return prototypeArtifactService.readFile(prdId, "preview/index.html");
    } catch (CustomException e) {
      if (job.getPrdMarkdown() == null || job.getPrdMarkdown().isBlank()) {
        throw e;
      }
      return """
          <!doctype html>
          <html lang="ko">
            <head><meta charset="UTF-8" /><meta name="viewport" content="width=device-width, initial-scale=1.0" /><title>PRD Preview</title></head>
            <body style="font-family:system-ui;padding:20px;background:#f8fafc;">
              <pre style="white-space:pre-wrap;background:#fff;border:1px solid #e2e8f0;border-radius:12px;padding:16px;">%s</pre>
            </body>
          </html>
          """
          .formatted(escapeHtml(job.getPrdMarkdown()));
    }
  }

  @Transactional(readOnly = true)
  public List<PrototypeJobSummaryResponse> listJobs(Long userId, Long ideaId) {
    Idea idea = loadIdeaAndCheckAccess(userId, ideaId);
    return jobRepository.findByIdea_IdOrderByIdDesc(idea.getId()).stream()
        .map(j -> toSummary(j, idea.getId()))
        .toList();
  }

  private Idea loadIdeaAndCheckAccess(Long userId, Long ideaId) {
    Idea idea =
        ideaRepository.findById(ideaId).orElseThrow(() -> new CustomException(NOT_FOUND_IDEA));
    ensureWorkspaceAccess(userId, idea.getWorkspace());
    return idea;
  }

  private void ensureWorkspaceAccess(Long userId, Workspace workspace) {
    if (localDevPermitAllWorkspaceAccess) {
      log.warn(
          "workspace access bypassed by local-dev flag userId={} workspaceId={}",
          userId,
          workspace != null ? workspace.getWorkspaceId() : null);
      return;
    }
    var user = userRepository.findById(userId).orElseThrow(() -> new CustomException(NOT_FOUND_USER));
    boolean member = workspaceUserRepository.existsByWorkspaceAndUser(workspace, user);
    if (!member) {
      var memberIds =
          workspaceUserRepository.findByWorkspace(workspace).stream()
              .map(wu -> wu.getUser().getId())
              .toList();
      log.warn(
          "workspace access denied in PRD flow userId={} workspaceId={} workspaceOwnerId={} memberIds={}",
          userId,
          workspace.getWorkspaceId(),
          workspace.getOwner() != null ? workspace.getOwner().getId() : null,
          memberIds);
      throw new CustomException(FORBIDDEN_WORKSPACE_ACCESS);
    }
  }

  private Idea requireWorkspaceScopedIdea(IdeaPrototypeJob job, Long workspaceId) {
    Idea idea = job.getIdea();
    if (idea == null
        || idea.getWorkspace() == null
        || !Objects.equals(idea.getWorkspace().getWorkspaceId(), workspaceId)) {
      throw new CustomException(NOT_FOUND_PROTOTYPE_JOB_BY_ID);
    }
    return idea;
  }

  private static String escapeHtml(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }

  private PrototypeJobSummaryResponse toSummary(IdeaPrototypeJob job, long ideaId) {
    String err = job.getErrorMessage();
    String preview =
        err == null
            ? null
            : (err.length() > 200 ? err.substring(0, 200) + "…" : err);
    return PrototypeJobSummaryResponse.builder()
        .prdId(job.getId())
        .jobId(job.getId())
        .ideaId(ideaId)
        .status(job.getStatus())
        .createdAt(toInstant(job.getCreatedAt()))
        .errorPreview(preview)
        .build();
  }

  private PrototypePipelineResponse toResponse(
      IdeaPrototypeJob job, Idea idea, Long ideaId, boolean includePrdMarkdown) {
    long workspaceId = idea.getWorkspace().getWorkspaceId();
    String prdViewPath = "/prd/workspaces/" + workspaceId + "/prds/" + job.getId();
    String prdViewUrl = buildPrdPublicUrl(prdViewPath);
    String message = buildMessage(job);
    return PrototypePipelineResponse.builder()
        .workspaceId(workspaceId)
        .prdViewPath(prdViewPath)
        .prdViewUrl(prdViewUrl)
        .prdId(job.getId())
        .jobId(job.getId())
        .ideaId(ideaId)
        .status(job.getStatus())
        .prdMarkdown(includePrdMarkdown ? job.getPrdMarkdown() : null)
        .uiStructureJson(job.getUiStructureJson())
        .githubRepoUrl(job.getGithubRepoUrl())
        .vercelPreviewUrl(job.getVercelPreviewUrl())
        .vercelProductionUrl(job.getVercelProductionUrl())
        .simulated(Boolean.TRUE.equals(job.getSimulated()))
        .vercelDeploymentApiUsed(job.getVercelDeploymentApiUsed())
        .message(message)
        .createdAt(toInstant(job.getCreatedAt()))
        .updatedAt(toInstant(job.getUpdatedAt()))
        .build();
  }

  private String buildPrdPublicUrl(String path) {
    if (path == null || path.isBlank()) {
      return null;
    }
    String base = prdPublicBaseUrl != null ? prdPublicBaseUrl.trim() : "http://localhost:3000";
    while (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    String p = path.startsWith("/") ? path : "/" + path;
    return base + p;
  }

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }

  private static String buildMessage(IdeaPrototypeJob job) {
    if (job.getStatus() == PrototypeJobStatus.FAILED) {
      return job.getErrorMessage() != null ? job.getErrorMessage() : "실패";
    }
    if (job.getStatus() == PrototypeJobStatus.PENDING || job.getStatus() == PrototypeJobStatus.RUNNING) {
      return "처리 중입니다.";
    }
    boolean sim = Boolean.TRUE.equals(job.getSimulated());
    boolean api = Boolean.TRUE.equals(job.getVercelDeploymentApiUsed());
    if (sim) {
      return "Vercel 계정 연결/설정이 없어 시뮬레이션 URL을 반환했습니다. APP_PROTOTYPE_VERCEL_* 설정을 확인하세요.";
    }
    if (api) {
      return "Vercel 배포 API로 배포를 생성했습니다. 프리뷰 URL을 확인하세요.";
    }
    return "파이프라인이 완료되었습니다. Vercel Import 링크가 포함될 수 있습니다.";
  }
}
