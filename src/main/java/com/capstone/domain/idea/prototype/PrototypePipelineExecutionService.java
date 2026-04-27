package com.capstone.domain.idea.prototype;

import com.capstone.domain.idea.Idea;
import com.capstone.domain.idea.IdeaRepository;
import com.capstone.global.service.WebSocketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrototypePipelineExecutionService {

  @Value("${app.prd-public-base-url:http://localhost:3000}")
  private String prdPublicBaseUrl;

  /** 동일 아이디어에 대한 파이프라인은 순차 실행 (재생성·동시 실행 충돌 방지) */
  private static final ConcurrentHashMap<Long, Object> PIPELINE_LOCK_BY_IDEA_ID =
      new ConcurrentHashMap<>();

  private final IdeaRepository ideaRepository;
  private final IdeaPrototypeJobRepository jobRepository;
  private final PrototypeAiContentService prototypeAiContentService;
  private final ReactPrototypeGenerator reactPrototypeGenerator;
  private final VercelPrototypeDeployService vercelPrototypeDeployService;
  private final PrototypeArtifactService prototypeArtifactService;
  private final WebSocketService webSocketService;
  private final ObjectMapper objectMapper;

  @Transactional
  public void executePipeline(Long jobId) {
    IdeaPrototypeJob jobSnapshot =
        jobRepository.findById(jobId).orElseThrow(() -> new IllegalStateException("job not found"));
    if (jobSnapshot.getIdea() == null) {
      fail(jobSnapshot, "idea reference missing");
      return;
    }
    Long ideaId = jobSnapshot.getIdea().getId();
    Object lock = PIPELINE_LOCK_BY_IDEA_ID.computeIfAbsent(ideaId, k -> new Object());
    synchronized (lock) {
      IdeaPrototypeJob job =
          jobRepository.findById(jobId).orElseThrow(() -> new IllegalStateException("job not found"));
      if (job.getIdea() == null) {
        fail(job, "idea reference missing");
        return;
      }
      if (job.getStatus() == PrototypeJobStatus.FAILED) {
        log.info("prototype skip already failed jobId={}", jobId);
        return;
      }
      Idea idea = ideaRepository.findById(job.getIdea().getId()).orElse(null);
      if (idea == null) {
        fail(job, "idea not found for job");
        return;
      }

      try {
        job.setStatus(PrototypeJobStatus.RUNNING);
        jobRepository.saveAndFlush(job);

        String prd = prototypeAiContentService.generatePrd(idea);
        job.setPrdMarkdown(prd);
        job.setStatus(PrototypeJobStatus.PRD_GENERATED);
        jobRepository.saveAndFlush(job);

        String ui = prototypeAiContentService.generateUiJson(idea, prd);
        job.setUiStructureJson(ui);
        job.setStatus(PrototypeJobStatus.UI_GENERATED);
        jobRepository.saveAndFlush(job);

        Map<String, String> files = reactPrototypeGenerator.generateFiles(idea);
        job.setGeneratedFilesSummaryJson(buildFilesSummary(files));
        job.setStatus(PrototypeJobStatus.CODE_GENERATED);
        jobRepository.saveAndFlush(job);

        try {
          prototypeArtifactService.writeForJob(job.getId(), files);
        } catch (Exception e) {
          log.warn("prototype disk artifact write failed (pipeline continues) jobId={}", job.getId(), e);
        }

        Long iid = idea.getId();
        String repoName = "idea-prototype-" + iid + "-" + job.getId();
        String projectSlug = sanitizeProjectSlug(repoName);
        VercelPrototypeDeployService.VercelResolution urls =
            vercelPrototypeDeployService.resolveUrls(files, job.getId(), projectSlug);

        job.setVercelPreviewUrl(urls.previewUrl());
        job.setVercelProductionUrl(urls.productionUrl());
        job.setSimulated(urls.simulated());
        job.setVercelDeploymentApiUsed(urls.deploymentApiUsed());
        job.setStatus(PrototypeJobStatus.DEPLOYED);
        jobRepository.saveAndFlush(job);

        long workspaceId = idea.getWorkspace().getWorkspaceId();
        String prdViewPath = "/prd/workspaces/" + workspaceId + "/prds/" + job.getId();
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("type", "prototype_ready");
        wsPayload.put("ideaId", iid);
        wsPayload.put("jobId", job.getId());
        wsPayload.put("workspaceId", workspaceId);
        wsPayload.put("prdViewPath", prdViewPath);
        wsPayload.put("prdViewUrl", buildPrdPublicUrl(prdViewPath));
        webSocketService.broadcastToWorkspace(workspaceId, "prototype", wsPayload);
      } catch (Exception e) {
        log.error("executePipeline failed jobId={}", jobId, e);
        fail(job, e.getMessage());
      }
    }
  }

  private void fail(IdeaPrototypeJob job, String message) {
    job.setStatus(PrototypeJobStatus.FAILED);
    job.setErrorMessage(message != null ? message : "unknown error");
    jobRepository.save(job);
  }

  private String buildFilesSummary(Map<String, String> files) {
    try {
      ObjectNode root = objectMapper.createObjectNode();
      for (Map.Entry<String, String> e : files.entrySet()) {
        root.put(e.getKey(), e.getValue().length());
      }
      return objectMapper.writeValueAsString(root);
    } catch (Exception e) {
      Map<String, Integer> m = new LinkedHashMap<>();
      files.forEach((k, v) -> m.put(k, v.length()));
      return m.toString();
    }
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

  private static String sanitizeProjectSlug(String name) {
    String s = name.toLowerCase().replaceAll("[^a-z0-9-]", "-");
    if (s.length() > 48) {
      s = s.substring(0, 48);
    }
    return s.replaceAll("-+", "-").replaceAll("^-|-$", "");
  }
}
