package com.capstone.domain.idea.prototype.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PrototypeSourceFileResponse {
  private final String path;
  private final String content;
}
