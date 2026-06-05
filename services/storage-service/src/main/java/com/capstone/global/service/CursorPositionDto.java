package com.capstone.global.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPositionDto {
  private Long userId;
  private Long workspaceId;
  private Double x;
  private Double y;
  private String userName; // 선택사항: 사용자 이름 표시용
  private Long timestamp; // 선택사항: 타임스탬프
}

