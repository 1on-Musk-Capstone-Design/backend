package com.capstone.domain.user;

import com.capstone.domain.workspaceUser.WorkspaceUser;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import com.capstone.global.exception.CustomException;
import com.capstone.global.exception.ErrorCode;
import com.capstone.global.oauth.JwtProvider;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final WorkspaceUserRepository workspaceUserRepository;
  private final JwtProvider jwtProvider;

  public UserResponse getCurrentUser(String accessToken) {
    try {
      Long userId = jwtProvider.getUserIdFromAccessToken(accessToken);
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

      return UserResponse.from(user);
    } catch (JwtException | IllegalArgumentException e) {
      throw new CustomException(ErrorCode.INVALID_TOKEN);
    }
  }

  @Transactional
  public UserResponse updateUser(String accessToken, UserUpdateRequest request) {
    try {
      Long userId = jwtProvider.getUserIdFromAccessToken(accessToken);
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

      // name이 제공된 경우에만 업데이트
      if (request.getName() != null && !request.getName().trim().isEmpty()) {
        user.setName(request.getName().trim());
      }

      // profileImage가 제공된 경우 업데이트 (null도 허용하여 프로필 이미지 제거 가능)
      if (request.getProfileImage() != null) {
        user.setProfileImage(request.getProfileImage());
      }

      User updatedUser = userRepository.saveAndFlush(user);
      return UserResponse.from(updatedUser);
    } catch (JwtException | IllegalArgumentException e) {
      throw new CustomException(ErrorCode.INVALID_TOKEN);
    }
  }

  @Transactional
  public void deleteUser(String accessToken) {
    try {
      Long userId = jwtProvider.getUserIdFromAccessToken(accessToken);
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

      List<WorkspaceUser> memberships = workspaceUserRepository.findByUser(user);
      if (!memberships.isEmpty()) {
        workspaceUserRepository.deleteAll(memberships);
        log.info("유저({})의 워크스페이스 멤버십 {}건 삭제 완료", userId, memberships.size());
      }

      user.setRefreshToken(null);
      userRepository.delete(user);

    } catch (JwtException | IllegalArgumentException e) {
      throw new CustomException(ErrorCode.INVALID_TOKEN);
    } catch (Exception e) {
      log.error("유저 삭제 중 예기치 못한 오류 발생: {}", e.getMessage());
      throw e;
    }
  }
}
