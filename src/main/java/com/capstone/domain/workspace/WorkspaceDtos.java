package com.capstone.domain.workspace;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

public class WorkspaceDtos {

  @Setter
  @Getter
  public static class CreateRequest {

    private String name;

  }

  @Setter
  @Getter
  public static class Response {

    private Long workspaceId;
    private String name;
    private Instant createdAt;

  }

  @Setter
  @Getter
  public static class ListItem {

    private Long workspaceId;
    private String name;
    private Instant createdAt;

  }

  @Setter
  @Getter
  public static class UpdateRequest {

    private String name;

  }

  @Setter
  @Getter
  public static class DeleteResponse {

    private String message;

  }

  @Setter
  @Getter
  public static class InviteLinkResponse {

    private String token;
    private String inviteUrl;
    private Instant expiresAt;
  }

  @Setter
  @Getter
  public static class InviteAcceptRequest {

    /**
     * 초대 링크를 통해 참여할 사용자 ID (Authorization 헤더가 없는 경우 필수)
     */
    private Long userId;
  }
}
