package com.capstone.domain.voicesession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoiceSessionRepository extends JpaRepository<VoiceSession, Long> {
    List<VoiceSession> findByWorkspace_WorkspaceId(Long workspaceId); // workspaceId를 기준으로 검색
}