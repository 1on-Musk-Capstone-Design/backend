package com.capstone.global.oauth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DevPreviewBootstrapRequest {

  private Long workspaceId;
  private String browserSessionId;
}