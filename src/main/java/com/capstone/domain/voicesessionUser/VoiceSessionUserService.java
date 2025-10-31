package com.capstone.domain.voicesessionUser;

import com.capstone.domain.voicesession.VoiceSession;
import com.capstone.domain.voicesession.VoiceSessionRepository;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoiceSessionUserService {

    private final VoiceSessionUserRepository voiceSessionUserRepository;
    private final VoiceSessionRepository sessionRepository;
    private final WorkspaceUserRepository workspaceUserRepository;

    /**
     * 세션 참여
     */
    @Transactional
    public VoiceSessionUser joinSession(Long workspaceId, Long sessionId, Long workspaceUserId) {
        // 1. WorkspaceUser 조회
        WorkspaceUser workspaceUser = workspaceUserRepository.findById(workspaceUserId)
                .orElseThrow(() -> new IllegalArgumentException("해당 워크스페이스 유저를 찾을 수 없습니다."));

        // 2. 워크스페이스 일치 확인
        if (!workspaceUser.getWorkspace().getWorkspaceId().equals(workspaceId)) {
            throw new IllegalArgumentException("이 유저는 해당 워크스페이스에 속하지 않습니다.");
        }

        // 3. 세션 조회
        VoiceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("해당 음성 세션을 찾을 수 없습니다."));

        // 4. 세션이 해당 워크스페이스에 속하는지 확인
        if (!session.getWorkspace().getWorkspaceId().equals(workspaceId)) {
            throw new IllegalArgumentException("이 세션은 해당 워크스페이스에 속하지 않습니다.");
        }

        // 5. 세션 종료 여부 확인
        if (session.getEndedAt() != null) {
            throw new IllegalStateException("종료된 세션에는 참여할 수 없습니다.");
        }

        // 6. 이미 참여 중인지 확인
        Optional<VoiceSessionUser> existing = voiceSessionUserRepository
                .findBySessionIdAndWorkspaceUserIdAndLeftAtIsNull(sessionId, workspaceUserId);

        if (existing.isPresent()) {
            throw new IllegalStateException("이미 이 세션에 참여 중입니다.");
        }

        // 7. 참여자 생성
        VoiceSessionUser voiceSessionUser = VoiceSessionUser.builder()
                .session(session)
                .workspaceUser(workspaceUser)
                .build();

        return voiceSessionUserRepository.save(voiceSessionUser);
    }

    /**
     * 세션 퇴장
     */
    @Transactional
    public VoiceSessionUser leaveSession(Long workspaceId, Long sessionId, Long workspaceUserId) {
        VoiceSessionUser voiceSessionUser = voiceSessionUserRepository
                .findBySessionIdAndWorkspaceUserIdAndLeftAtIsNull(sessionId, workspaceUserId)
                .orElseThrow(() -> new IllegalArgumentException("해당 세션에서 사용자를 찾을 수 없거나 이미 퇴장 했습니다."));

        // 워크스페이스 일치 확인
        if (!voiceSessionUser.getWorkspaceUser().getWorkspace().getWorkspaceId().equals(workspaceId)) {
            throw new IllegalArgumentException("이 세션은 해당 워크스페이스에 속하지 않습니다.");
        }

        // 퇴장 처리
        voiceSessionUser.leave();

        return voiceSessionUserRepository.save(voiceSessionUser);
    }

    /**
     * 채널 이동
     */
    @Transactional
    public VoiceSessionUser moveToSession(Long workspaceId, Long fromSessionId, Long toSessionId, Long workspaceUserId) {
        try {
            leaveSession(workspaceId, fromSessionId, workspaceUserId);
        } catch (IllegalArgumentException e) {
            // 무시
        }
        return joinSession(workspaceId, toSessionId, workspaceUserId);
    }

    /**
     * 현재 참여자 조회
     */
    public List<VoiceSessionUser> getActiveUsers(Long workspaceId, Long sessionId) {
        VoiceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("해당 음성세션을 찾을 수 없습니다."));

        if (!session.getWorkspace().getWorkspaceId().equals(workspaceId)) {
            throw new IllegalArgumentException("이 세션은 해당 워크스페이스에 속하지 않습니다.");
        }

        return voiceSessionUserRepository.findBySessionIdAndLeftAtIsNull(sessionId);
    }

    /**
     * 전체 참여자 조회
     */
    public List<VoiceSessionUser> getAllUsers(Long workspaceId, Long sessionId) {
        VoiceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("해당 음성세션을 찾을 수 없습니다."));

        if (!session.getWorkspace().getWorkspaceId().equals(workspaceId)) {
            throw new IllegalArgumentException("이 세션은 해당 워크스페이스에 속하지 않습니다.");
        }

        return voiceSessionUserRepository.findBySessionId(sessionId);
    }

    /**
     * 현재 참여자 수 조회
     */
    public long getActiveUserCount(Long workspaceId, Long sessionId) {
        VoiceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("해당 음성세션을 찾을 수 없습니다."));

        if (!session.getWorkspace().getWorkspaceId().equals(workspaceId)) {
            throw new IllegalArgumentException("이 세션은 해당 워크스페이스에 속하지 않습니다.");
        }

        return voiceSessionUserRepository.countBySessionIdAndLeftAtIsNull(sessionId);
    }
}
