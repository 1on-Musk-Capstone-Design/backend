package com.capstone.domain.voicesessionUser;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "음성 세션 참여자 요청 DTO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VoiceSessionUserRequest {

    @Schema(description = "워크스페이스 사용자 ID", example = "5", required = true)
    private Long workspaceUserId;
}