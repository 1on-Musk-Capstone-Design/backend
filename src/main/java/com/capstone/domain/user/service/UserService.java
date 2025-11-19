package com.capstone.domain.user.service;

import com.capstone.domain.user.dto.UserResponse;
import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.global.exception.CustomException;
import com.capstone.global.exception.ErrorCode;
import com.capstone.global.oauth.JwtProvider;
import io.jsonwebtoken.JwtException;
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
}

