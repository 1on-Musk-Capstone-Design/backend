package com.capstone.domain.voicesession;

import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VoiceSessionService {

    private final VoiceSessionRepository repository;
    private final WorkspaceRepository workspaceRepository;

    public VoiceSessionService(VoiceSessionRepository repository, WorkspaceRepository workspaceRepository) {
        this.repository = repository;
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional
    public VoiceSession startSession(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found with id: " + workspaceId));

        VoiceSession session = new VoiceSession(workspace, LocalDateTime.now());
        return repository.save(session);
    }

    @Transactional
    public VoiceSession endSession(Long sessionId) {
        VoiceSession session = repository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("VoiceSession not found with id: " + sessionId));
        session.setEndedAt(LocalDateTime.now());
        return repository.save(session);
    }

    @Transactional(readOnly = true)
    public List<VoiceSession> getAllSessions(Long workspaceId) {
        return repository.findByWorkspace_WorkspaceId(workspaceId); 
    }
}