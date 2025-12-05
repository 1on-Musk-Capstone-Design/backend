package com.capstone.domain.user.service;

import com.capstone.domain.user.dto.UserResponse;
import com.capstone.domain.user.dto.UserUpdateRequest;
import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.global.exception.CustomException;
import com.capstone.global.exception.ErrorCode;
import com.capstone.global.oauth.JwtProvider;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
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
}

