package com.capstone.domain.idea;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IdeaResponse {

  private Long id;
  private Long workspaceId;
  private Long canvasId;
  private String content;
  private Double patchSizeX;
  private Double patchSizeY;
  private Double positionX;
  private Double positionY;
  private String createdAt;
  private String updatedAt;

  public static IdeaResponse from(Idea idea) {
    return IdeaResponse.builder()
        .id(idea.getId())
        .workspaceId(idea.getWorkspace().getWorkspaceId())
        .canvasId(idea.getCanvas() != null ? idea.getCanvas().getId() : null)
        .content(idea.getContent())
        .patchSizeX(idea.getPatchSizeX())
        .patchSizeY(idea.getPatchSizeY())
        .positionX(idea.getPositionX())
        .positionY(idea.getPositionY())
        .createdAt(idea.getCreatedAt().toString())
        .updatedAt(idea.getUpdatedAt().toString())
        .build();
  }
}
