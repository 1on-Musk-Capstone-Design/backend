package com.capstone.global.oauth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DevBootstrapSessionDto {

  private final String accessToken;
  private final String refreshToken;
  private final String name;
  private final String email;
}
