package com.capstone.domain.canvas;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CanvasRequest {

  private Long workspaceId;
  private String title;
}
