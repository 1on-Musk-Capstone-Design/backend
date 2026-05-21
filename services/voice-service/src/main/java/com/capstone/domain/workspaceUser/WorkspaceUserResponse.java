package com.capstone.domain.workspaceUser;

import com.capstone.global.type.Role;
import java.sql.Timestamp;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WorkspaceUserResponse {

  private Long id;
  private String email;
  private String name;
  private String profileImage;
  private Role role;
  private Timestamp joinedAt;
}