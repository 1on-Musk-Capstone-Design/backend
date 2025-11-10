package com.capstone.domain.voicesession;

import com.capstone.domain.workspace.Workspace;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "voice_session")
public class VoiceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wrokspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    public VoiceSession() {
    }

    public VoiceSession(Workspace workspace, LocalDateTime startedAt) {
        this.workspace = workspace;
        this.startedAt = startedAt;
    }

}