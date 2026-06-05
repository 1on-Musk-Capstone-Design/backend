package com.capstone.domain.idea.prototype;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrototypePipelineRunner {

  private final PrototypePipelineExecutionService executionService;

  @Async("prototypeTaskExecutor")
  public void runPipelineAsync(Long jobId) {
    try {
      executionService.executePipeline(jobId);
    } catch (Exception e) {
      log.error("prototype async pipeline failed jobId={}", jobId, e);
    }
  }
}
