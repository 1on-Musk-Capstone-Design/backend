package com.capstone.domain.canvas;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CanvasResponse {

  private Long id;
  private String title;
  private String createdAt;
  private String updatedAt;
}
