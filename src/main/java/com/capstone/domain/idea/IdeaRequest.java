package com.capstone.domain.idea;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdeaRequest {

  private Long canvasId;
  private Long workspaceId;
  private String content;
  private Double positionX;
  private Double positionY;
}
