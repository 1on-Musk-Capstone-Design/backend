package com.capstone.global.oauth.service;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.global.oauth.JwtProvider;
import com.capstone.global.oauth.TokenDto;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleService {

  private final UserRepository userRepository;
  private final JwtProvider jwtProvider;
  private final RestTemplate restTemplate;

  @Value("${oauth2.google.client-id}")
  private String clientId;

  @Value("${oauth2.google.client-secret}")
  private String clientSecret;

  @Value("${oauth2.google.redirect-uri}")
  private String redirectUri;

  @Value("${oauth2.google.token-uri}")
  private String tokenUri;

  @Value("${oauth2.google.resource-uri}")
  private String resourceUri;

  @Transactional
  public TokenDto loginOrJoin(String code) {
    log.info("OAuth 로그인 시작 - code: {}", code != null ? code.substring(0, Math.min(10, code.length())) + "..." : "null");
    
    try {
      String accessToken = getAccessToken(code);
      log.info("Access token 획득 성공");
      
      JsonNode userInfo = getUserInfo(accessToken);
      log.info("사용자 정보 획득 성공");

      String email = userInfo.get("email").asText();
      String name = userInfo.get("name").asText();
      String picture = userInfo.get("picture") != null ? userInfo.get("picture").asText() : null;
      
      log.info("사용자 정보 - email: {}, name: {}", email, name);

      User user = userRepository.findByEmail(email.trim())
          .orElseGet(() -> {
            log.info("새 사용자 생성 - email: {}, name: {}", email, name);
            User newUser = User.builder()
                .email(email)
                .name(name)
                .profileImage(picture)
                .build();
            User saved = userRepository.save(newUser);
            log.info("사용자 저장 완료 - id: {}, email: {}", saved.getId(), saved.getEmail());
            return saved;
          });

      log.info("기존/신규 사용자 확인 - id: {}, email: {}", user.getId(), user.getEmail());

      String jwtAccessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail());
      String jwtRefreshToken = jwtProvider.createRefreshToken(user.getId(), user.getEmail());

      user.setRefreshToken(jwtRefreshToken);
      User updatedUser = userRepository.save(user);
      log.info("Refresh token 저장 완료 - id: {}, email: {}", updatedUser.getId(), updatedUser.getEmail());

      return TokenDto.builder()
          .accessToken(jwtAccessToken)
          .refreshToken(jwtRefreshToken)
          .build();
    } catch (Exception e) {
      log.error("OAuth 로그인 처리 중 오류 발생", e);
      throw e;
    }
  }

  private String getAccessToken(String code) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("code", code);
    params.add("client_id", clientId);
    params.add("client_secret", clientSecret);
    params.add("redirect_uri", redirectUri);
    params.add("grant_type", "authorization_code");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
    JsonNode response = restTemplate.exchange(tokenUri, HttpMethod.POST, entity, JsonNode.class)
        .getBody();

    return Objects.requireNonNull(response).get("access_token").asText();
  }

  private JsonNode getUserInfo(String accessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + accessToken);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    return restTemplate.exchange(resourceUri, HttpMethod.GET, entity, JsonNode.class).getBody();
  }
}
