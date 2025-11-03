package com.capstone.domain.voicesessionUser;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Schema(description = "음성 세션 참여자 응답 DTO")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceSessionUserResponse {

    @Schema(description = "참여 ID", example = "1")
    private Long id;

    @Schema(description = "세션 ID", example = "5")
    private Long sessionId;

    @Schema(description = "워크스페이스 사용자 ID", example = "10")
    private Long workspaceUserId;

    @Schema(description = "워크스페이스 사용자 이름", example = "홍길동")
    private String workspaceUserName;

    @Schema(description = "참여 시간", example = "2025-10-13T10:30:00")
    private Timestamp joinedAt;

    @Schema(description = "퇴장 시간", example = "2025-10-13T11:45:00", nullable = true)
    private LocalDateTime leftAt;

    @Schema(description = "활성 상태 (현재 참여 중)", example = "true")
    private boolean active;

    public static VoiceSessionUserResponse from(VoiceSessionUser voiceSessionUser) {
        return VoiceSessionUserResponse.builder()
                .id(voiceSessionUser.getId())
                .sessionId(voiceSessionUser.getSession().getId())
                .workspaceUserId(voiceSessionUser.getWorkspaceUser().getId())
                .workspaceUserName(voiceSessionUser.getWorkspaceUser().getUser().getName())
                .joinedAt(Timestamp.valueOf(voiceSessionUser.getJoinedAt()))
                .leftAt(voiceSessionUser.getLeftAt())
                .active(voiceSessionUser.isActive())
                .build();
    }
}