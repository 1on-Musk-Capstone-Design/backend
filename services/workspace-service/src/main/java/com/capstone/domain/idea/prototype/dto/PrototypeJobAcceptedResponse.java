package com.capstone.domain.idea.prototype.dto;

import com.capstone.domain.idea.prototype.PrototypeJobStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PrototypeJobAcceptedResponse {

  private final Long jobId;
  private final Long ideaId;
  private final PrototypeJobStatus status;
  private final String message;
}
