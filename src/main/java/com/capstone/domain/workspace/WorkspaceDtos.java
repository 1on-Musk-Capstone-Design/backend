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
}
