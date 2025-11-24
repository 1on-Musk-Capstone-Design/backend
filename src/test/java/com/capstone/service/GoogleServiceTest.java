package com.capstone.service;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.global.oauth.JwtProvider;
import com.capstone.global.oauth.TokenDto;
import com.capstone.global.oauth.service.GoogleService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private JwtProvider jwtProvider;

  @Mock
  private RestTemplate restTemplate;

  @InjectMocks
  private GoogleService googleService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(googleService, "clientId", "capstone-id");
    ReflectionTestUtils.setField(googleService, "clientSecret", "capstone-secret");
    ReflectionTestUtils.setField(googleService, "redirectUri", "http://localhost/capstone");
    ReflectionTestUtils.setField(googleService, "tokenUri", "http://capstone-token-uri");
    ReflectionTestUtils.setField(googleService, "resourceUri", "http://capstone-resource-uri");

    when(jwtProvider.createAccessToken(anyLong(), anyString())).thenReturn("access");
    when(jwtProvider.createRefreshToken(anyLong(), anyString())).thenReturn("refresh");

    lenient().when(jwtProvider.createAccessToken(anyLong(), anyString())).thenReturn("access");
    lenient().when(jwtProvider.createRefreshToken(anyLong(), anyString()))
        .thenReturn("refresh");
  }

  @Test
  @DisplayName("로그인 & 회원가입 성공 테스트")
  void successLoginOrJoin() throws Exception {
    User existingUser = User.builder()
        .id(1L)
        .email("capstone@test.com")
        .name("cap")
        .profileImage("profile.png")
        .build();

    JsonNode tokenResponse = objectMapper.readTree("{\"access_token\":\"mock-access-token\"}");
    when(restTemplate.exchange(eq("http://capstone-token-uri"), eq(HttpMethod.POST),
        any(HttpEntity.class), eq(JsonNode.class)))
        .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

    JsonNode userInfoResponse = objectMapper.readTree(
        "{\"email\":\"capstone@test.com\",\"name\":\"cap\",\"picture\":\"profile.png\"}");
    when(restTemplate.exchange(eq("http://capstone-resource-uri"), eq(HttpMethod.GET),
        any(HttpEntity.class), eq(JsonNode.class)))
        .thenReturn(new ResponseEntity<>(userInfoResponse, HttpStatus.OK));

    when(userRepository.findByEmail("capstone@test.com")).thenReturn(Optional.of(existingUser));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    TokenDto tokenDto = googleService.loginOrJoin("auth-code", "http://localhost/capstone");

    assertNotNull(tokenDto);
    assertEquals("access", tokenDto.getAccessToken());
    assertEquals("refresh", tokenDto.getRefreshToken());

    verify(userRepository, times(1)).findByEmail("capstone@test.com");
    verify(userRepository, times(1)).save(existingUser);
  }

  @Test
  @DisplayName("로그인 & 회원가입 실패 테스트")
  void failLoginOrJoin() {
    when(restTemplate.exchange(
        anyString(),
        eq(HttpMethod.POST),
        any(HttpEntity.class),
        eq(JsonNode.class)
    )).thenThrow(new RuntimeException("capstone-token-uri 실패"));

    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> googleService.loginOrJoin("code", "http://localhost/capstone"));
    assertEquals("capstone-token-uri 실패", exception.getMessage());
  }
}
