package com.capstone.domain.user.dto;

import com.capstone.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

  private final Long id;
  private final String email;
  private final String name;
  private final String profileImage;

  public static UserResponse from(User user) {
    return UserResponse.builder()
        .id(user.getId())
        .email(user.getEmail())
        .name(user.getName())
        .profileImage(user.getProfileImage())
        .build();
  }
}

