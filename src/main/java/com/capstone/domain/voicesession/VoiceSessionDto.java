package com.capstone.domain.voicesession;

import com.capstone.domain.workspace.Workspace;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VoiceSessionDto {

  private Long id;
  private Long workspaceId;
  private LocalDateTime startedAt;
  private LocalDateTime endedAt;

  private VoiceSessionDto() {
  }

  public static VoiceSessionDto from(VoiceSession entity) {
    VoiceSessionDto dto = new VoiceSessionDto();
    dto.id = entity.getId();
    Workspace ws = entity.getWorkspace();
    if (ws != null) {
      dto.workspaceId = ws.getWorkspaceId();
    }
    dto.startedAt = entity.getStartedAt();
    dto.endedAt = entity.getEndedAt();
    return dto;
  }
}