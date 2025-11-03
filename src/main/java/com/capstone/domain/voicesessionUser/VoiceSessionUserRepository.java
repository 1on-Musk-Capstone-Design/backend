package com.capstone.domain.voicesessionUser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoiceSessionUserRepository extends JpaRepository<VoiceSessionUser, Long> {

    /**
     * 특정 세션의 활성 참여자 목록 조회
     */
    List<VoiceSessionUser> findBySessionIdAndLeftAtIsNull(Long sessionId);

    /**
     * 특정 세션의 모든 참여자 조회 (이력 포함)
     */
    List<VoiceSessionUser> findBySessionId(Long sessionId);

    /**
     * 사용자가 특정 세션에 활성 참여 중인지 확인
     */
    Optional<VoiceSessionUser> findBySessionIdAndWorkspaceUserIdAndLeftAtIsNull(Long sessionId, Long workspaceUserId);

    /**
     * 특정 세션의 활성 참여자 수
     */
    long countBySessionIdAndLeftAtIsNull(Long sessionId);

    /**
     * N+1 문제 방지를 위한 Fetch Join (필요시 사용)
     */
    @Query("SELECT v FROM VoiceSessionUser v JOIN FETCH v.workspaceUser wu WHERE v.session.id = :sessionId AND v.leftAt IS NULL")
    List<VoiceSessionUser> findActiveUsersWithWorkspaceUserBySessionId(@Param("sessionId") Long sessionId);
}