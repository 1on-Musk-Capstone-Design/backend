package com.capstone.domain.idea.prototype.dto;

import com.capstone.domain.idea.prototype.PrototypeJobStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PrototypeJobAcceptedResponse {

  /** PRD 문서 식별자. 현재는 prototype job id와 동일합니다. */
  private final Long prdId;
  private final Long jobId;
  private final Long ideaId;
  private final PrototypeJobStatus status;
  private final String message;
}
