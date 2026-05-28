package com.capstone.global.oauth.service;

import com.capstone.domain.user.User;
import com.capstone.domain.user.UserRepository;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceRepository;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import com.capstone.global.exception.CustomException;
import com.capstone.global.exception.ErrorCode;
import com.capstone.global.oauth.JwtProvider;
import com.capstone.global.oauth.dto.DevBootstrapSessionDto;
import com.capstone.global.oauth.dto.DevPreviewBootstrapSessionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DevBootstrapService {

  private static final String DEV_EMAIL = "dev@localhost.local";
  private static final String DEV_NAME = "로컬 개발자";
  private static final String PREVIEW_EMAIL_DOMAIN = "preview.local";
  private static final String PREVIEW_NAME_PREFIX = "프리뷰 사용자";

  private final UserRepository userRepository;
  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceUserRepository workspaceUserRepository;
  private final JwtProvider jwtProvider;

  @Transactional
  public DevBootstrapSessionDto issueSession() {
    User user =
        userRepository
            .findTopByOrderByIdAsc()
            .orElseGet(
                () -> {
                  log.info("개발 부트스트랩: 사용자 없음 → {} 생성", DEV_EMAIL);
                  return userRepository.save(
                      User.builder().email(DEV_EMAIL).name(DEV_NAME).profileImage(null).build());
                });

    String access = jwtProvider.createAccessToken(user.getId(), user.getEmail());
    String refresh = jwtProvider.createRefreshToken(user.getId(), user.getEmail());
    user.setRefreshToken(refresh);
    userRepository.save(user);

    log.info("개발 부트스트랩: userId={} email={}", user.getId(), user.getEmail());

    return DevBootstrapSessionDto.builder()
        .accessToken(access)
        .refreshToken(refresh)
        .name(user.getName() != null ? user.getName() : DEV_NAME)
        .email(user.getEmail())
        .build();
  }

  @Transactional
  public DevPreviewBootstrapSessionDto issuePreviewSession(Long workspaceId, String browserSessionId) {
    if (workspaceId == null) {
      throw new CustomException(ErrorCode.BAD_REQUEST);
    }

    String normalizedBrowserSessionId = normalizeBrowserSessionId(browserSessionId);
    String previewEmail = buildPreviewEmail(normalizedBrowserSessionId);
    String previewName = buildPreviewName(normalizedBrowserSessionId);

    Workspace workspace = workspaceRepository.findById(workspaceId)
        .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WORKSPACE));

    User user = findOrCreatePreviewUser(previewEmail, previewName);

    WorkspaceUser workspaceUser = findOrCreatePreviewWorkspaceUser(workspace, user);

    String access = jwtProvider.createAccessToken(user.getId(), user.getEmail());
    String refresh = jwtProvider.createRefreshToken(user.getId(), user.getEmail());
    user.setRefreshToken(refresh);
    userRepository.save(user);

    log.info(
        "프리뷰 부트스트랩: workspaceId={} browserSessionId={} userId={} workspaceUserId={}",
        workspaceId,
        normalizedBrowserSessionId,
        user.getId(),
        workspaceUser.getId());

    return DevPreviewBootstrapSessionDto.builder()
        .accessToken(access)
        .refreshToken(refresh)
        .userId(user.getId())
        .workspaceUserId(workspaceUser.getId())
        .name(user.getName() != null ? user.getName() : previewName)
        .email(user.getEmail())
        .build();
  }

  private String normalizeBrowserSessionId(String browserSessionId) {
    if (browserSessionId == null || browserSessionId.isBlank()) {
      return java.util.UUID.randomUUID().toString();
    }
    return browserSessionId.trim();
  }

  private String buildPreviewEmail(String browserSessionId) {
    return "preview-" + browserSessionId + "@" + PREVIEW_EMAIL_DOMAIN;
  }

  private String buildPreviewName(String browserSessionId) {
    String shortId = browserSessionId.replaceAll("[^a-zA-Z0-9]", "");
    if (shortId.length() > 6) {
      shortId = shortId.substring(0, 6);
    }
    if (shortId.isBlank()) {
      shortId = "temp";
    }
    return PREVIEW_NAME_PREFIX + " " + shortId;
  }

  private User findOrCreatePreviewUser(String previewEmail, String previewName) {
    return userRepository.findByEmail(previewEmail)
        .map(existing -> {
          if (existing.getName() == null || existing.getName().isBlank()) {
            existing.setName(previewName);
          }
          return userRepository.save(existing);
        })
        .orElseGet(() -> createPreviewUser(previewEmail, previewName));
  }

  private User createPreviewUser(String previewEmail, String previewName) {
    try {
      return userRepository.save(
          User.builder()
              .email(previewEmail)
              .name(previewName)
              .profileImage(null)
              .build());
    } catch (DataIntegrityViolationException exception) {
      return userRepository.findByEmail(previewEmail).orElseThrow(() -> exception);
    }
  }

  private WorkspaceUser findOrCreatePreviewWorkspaceUser(Workspace workspace, User user) {
    return workspaceUserRepository.findByWorkspaceAndUser(workspace, user)
        .orElseGet(() -> createPreviewWorkspaceUser(workspace, user));
  }

  private WorkspaceUser createPreviewWorkspaceUser(Workspace workspace, User user) {
    try {
      return workspaceUserRepository.save(
          WorkspaceUser.builder()
              .workspace(workspace)
              .user(user)
              .role(com.capstone.global.type.Role.MEMBER)
              .build());
    } catch (DataIntegrityViolationException exception) {
      return workspaceUserRepository.findByWorkspaceAndUser(workspace, user)
          .orElseThrow(() -> exception);
    }
  }
}
