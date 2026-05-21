package com.capstone.domain.idea.prototype.dto;

import com.capstone.domain.idea.prototype.PrototypeJobStatus;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PrototypeJobSummaryResponse {

  private final Long jobId;
  private final Long ideaId;
  private final PrototypeJobStatus status;
  private final Instant createdAt;
  private final String errorPreview;
}
