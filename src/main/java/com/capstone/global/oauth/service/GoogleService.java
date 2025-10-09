package com.capstone.global.oauth.service;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.global.oauth.JwtProvider;
import com.capstone.global.oauth.TokenDto;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class GoogleService {

  private final UserRepository userRepository;
  private final JwtProvider jwtProvider;
  private  final RestTemplate restTemplate;

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

  public TokenDto loginOrJoin(String code) {
    String accessToken = getAccessToken(code);

    JsonNode userInfo = getUserResource(accessToken);

    String email = userInfo.get("email").asText();
    String name = userInfo.get("name").asText();
    String picture = userInfo.get("picture").asText();

    User user = userRepository.findByEmail(email.trim())
        .orElseGet(() -> userRepository.save(
            User.builder()
                .email(email)
                .name(name)
                .profileImage(picture)
                .build()
        ));

    String jwtAccessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail());
    String jwtRefreshToken = jwtProvider.createRefreshToken(user.getId(), user.getEmail());

    user.setRefreshToken(jwtRefreshToken);
    userRepository.save(user);

    return TokenDto.builder()
        .accessToken(jwtAccessToken)
        .refreshToken(jwtRefreshToken)
        .build();
  }

  public Long getUserIdFromToken(String token) {
    String accessToken = token.replace("Bearer ", "").trim();
    Long userId = jwtProvider.getUserIdFromAccessToken(accessToken);

    return userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("토큰 사용자 정보를 찾을 수 없습니다."))
        .getId();
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

  private JsonNode getUserResource(String accessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + accessToken);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    return restTemplate.exchange(resourceUri, HttpMethod.GET, entity, JsonNode.class).getBody();
  }


}
