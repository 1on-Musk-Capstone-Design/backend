package com.capstone.global.oauth.service;

import com.capstone.domain.user.User;
import com.capstone.domain.user.UserRepository;
import com.capstone.global.oauth.JwtProvider;
import com.capstone.global.oauth.TokenDto;
import com.capstone.global.oauth.dto.DevBootstrapSessionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DevBootstrapService {

  private static final String DEV_EMAIL = "dev@localhost.local";
  private static final String DEV_NAME = "로컬 개발자";

  private final UserRepository userRepository;
  private final JwtProvider jwtProvider;

  @Transactional
  public DevBootstrapSessionDto issueSession() {
    TokenDto tokenDto = issueToken();

    return DevBootstrapSessionDto.builder()
        .accessToken(tokenDto.getAccessToken())
        .refreshToken(tokenDto.getRefreshToken())
        .name(tokenDto.getName())
        .email(tokenDto.getEmail())
        .build();
  }

  @Transactional
  public TokenDto issueToken() {
    User user = userRepository.findByEmail(DEV_EMAIL)
        .orElseGet(
            () -> {
              log.info("개발 부트스트랩: {} 생성", DEV_EMAIL);
              return userRepository.save(
                  User.builder().email(DEV_EMAIL).name(DEV_NAME).profileImage(null).build());
            });

    String access = jwtProvider.createAccessToken(user.getId(), user.getEmail());
    String refresh = jwtProvider.createRefreshToken(user.getId(), user.getEmail());
    user.setRefreshToken(refresh);
    userRepository.save(user);

    log.info("개발 부트스트랩: userId={} email={}", user.getId(), user.getEmail());

    return TokenDto.builder()
        .accessToken(access)
        .refreshToken(refresh)
        .name(user.getName() != null ? user.getName() : DEV_NAME)
        .email(user.getEmail())
        .build();
  }
}
