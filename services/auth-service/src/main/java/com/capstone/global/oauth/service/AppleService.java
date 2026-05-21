package com.capstone.global.oauth.service;

import static com.capstone.global.exception.ErrorCode.*;

import com.capstone.domain.user.User;
import com.capstone.domain.user.UserRepository;
import com.capstone.global.exception.CustomException;
import com.capstone.global.oauth.AppleJwtValidator;
import com.capstone.global.oauth.JwtProvider;
import com.capstone.global.oauth.TokenDto;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppleService {

  private final UserRepository userRepository;
  private final JwtProvider jwtProvider;
  private final AppleJwtValidator appleJwtValidator;

  @Transactional
  public TokenDto loginOrJoin(String identityToken, String fullName) {
    try {
      Map<String, Object> claims = appleJwtValidator.validateAndGetClaims(identityToken);

      String email = (String) claims.get("email");

      String name = (fullName != null && !fullName.isBlank())
          ? fullName
          : (claims.get("name") != null ? (String) claims.get("name") : email.split("@")[0]);

      log.info("Apple 사용자 인증 성공: {}", email);

      User user = userRepository.findByEmail(email.trim())
          .orElseGet(() -> {
            try {
              return userRepository.save(User.builder()
                  .email(email.trim())
                  .name(name)
                  .build());
            } catch (DataIntegrityViolationException e) {
              return userRepository.findByEmail(email.trim()).orElseThrow();
            }
          });

      String accessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail());
      String refreshToken = jwtProvider.createRefreshToken(user.getId(), user.getEmail());

      user.setRefreshToken(refreshToken);
      userRepository.save(user);

      return TokenDto.builder()
          .accessToken(accessToken)
          .refreshToken(refreshToken)
          .name(user.getName())
          .email(user.getEmail())
          .build();

    } catch (IllegalArgumentException e) {
      log.error("Apple 토큰 검증 실패: {}", e.getMessage());
      throw new CustomException(INVALID_TOKEN);
    } catch (Exception e) {
      log.error("Apple 로그인 처리 중 시스템 오류 발생", e);
      throw new CustomException(APPLE_AUTH_FAILED);
    }
  }
}