package com.capstone.global.oauth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DevPreviewBootstrapSessionDto {

  private final String accessToken;
  private final String refreshToken;
  private final Long userId;
  private final Long workspaceUserId;
  private final String name;
  private final String email;
}