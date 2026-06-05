package com.capstone.service;

import com.capstone.domain.user.User;
import com.capstone.domain.user.UserRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
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
    ReflectionTestUtils.setField(googleService, "clientId", "capstone-web-client-id");
    ReflectionTestUtils.setField(googleService, "clientSecret", "capstone-secret");
    ReflectionTestUtils.setField(googleService, "iosClientId", "capstone-ios-client-id");
    ReflectionTestUtils.setField(googleService, "tokenUri", "http://capstone-token-uri");
    ReflectionTestUtils.setField(googleService, "resourceUri", "http://capstone-resource-uri");

    lenient().when(jwtProvider.createAccessToken(anyLong(), anyString())).thenReturn("access");
    lenient().when(jwtProvider.createRefreshToken(anyLong(), anyString())).thenReturn("refresh");
  }


  @Test
  @DisplayName("웹 로그인 성공 - 기존 유저")
  void successWebExistingUserLoginOrJoin() throws Exception {
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
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    TokenDto result = googleService.loginOrJoin("auth-code", "http://localhost/callback", false);

    assertNotNull(result);
    assertEquals("access", result.getAccessToken());
    assertEquals("refresh", result.getRefreshToken());
    assertEquals("cap", result.getName());
    assertEquals("capstone@test.com", result.getEmail());

    verify(userRepository, times(1)).findByEmail("capstone@test.com");
    verify(userRepository, times(1)).save(existingUser);
  }

  @Test
  @DisplayName("웹 로그인 성공 - 신규 유저 가입")
  void successWebNewUserLoginOrJoin() throws Exception {
    JsonNode tokenResponse = objectMapper.readTree("{\"access_token\":\"mock-access-token\"}");
    when(restTemplate.exchange(eq("http://capstone-token-uri"), eq(HttpMethod.POST),
        any(HttpEntity.class), eq(JsonNode.class)))
        .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

    JsonNode userInfoResponse = objectMapper.readTree(
        "{\"email\":\"new@test.com\",\"name\":\"newbie\",\"picture\":\"pic.png\"}");
    when(restTemplate.exchange(eq("http://capstone-resource-uri"), eq(HttpMethod.GET),
        any(HttpEntity.class), eq(JsonNode.class)))
        .thenReturn(new ResponseEntity<>(userInfoResponse, HttpStatus.OK));

    when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(inv -> {
      User u = inv.getArgument(0);
      return User.builder()
          .id(2L)
          .email(u.getEmail())
          .name(u.getName())
          .profileImage(u.getProfileImage())
          .build();
    });

    TokenDto result = googleService.loginOrJoin("auth-code", "http://localhost/callback", false);

    assertNotNull(result);
    assertEquals("access", result.getAccessToken());
    assertEquals("newbie", result.getName());
    assertEquals("new@test.com", result.getEmail());

    verify(userRepository, times(2)).save(any(User.class));
  }

  @Test
  @DisplayName("웹 로그인 성공 - 동시 요청으로 중복 저장 시도 시 기존 유저 조회로 복구")
  void successWebDuplicateLoginOrJoin() throws Exception {
    User existingUser = User.builder()
        .id(3L)
        .email("dup@test.com")
        .name("dupuser")
        .build();

    JsonNode tokenResponse = objectMapper.readTree("{\"access_token\":\"mock-access-token\"}");
    when(restTemplate.exchange(eq("http://capstone-token-uri"), eq(HttpMethod.POST),
        any(HttpEntity.class), eq(JsonNode.class)))
        .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

    JsonNode userInfoResponse = objectMapper.readTree(
        "{\"email\":\"dup@test.com\",\"name\":\"dupuser\",\"picture\":null}");
    when(restTemplate.exchange(eq("http://capstone-resource-uri"), eq(HttpMethod.GET),
        any(HttpEntity.class), eq(JsonNode.class)))
        .thenReturn(new ResponseEntity<>(userInfoResponse, HttpStatus.OK));

    when(userRepository.findByEmail("dup@test.com"))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(existingUser));
    when(userRepository.save(any(User.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate"))
        .thenAnswer(inv -> inv.getArgument(0));

    TokenDto result = googleService.loginOrJoin("auth-code", "http://localhost/callback", false);

    assertNotNull(result);
    assertEquals("dupuser", result.getName());
    verify(userRepository, times(2)).findByEmail("dup@test.com");
  }

  @Test
  @DisplayName("iOS 로그인 성공 - client_secret 없이 iosClientId 사용")
  void successIosNoClientSecreteLoginOrJoin() throws Exception {
    User existingUser = User.builder()
        .id(4L)
        .email("ios@test.com")
        .name("iosCap")
        .build();

    JsonNode tokenResponse = objectMapper.readTree("{\"access_token\":\"ios-mock-token\"}");
    when(restTemplate.exchange(eq("http://capstone-token-uri"), eq(HttpMethod.POST),
        any(HttpEntity.class), eq(JsonNode.class)))
        .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

    JsonNode userInfoResponse = objectMapper.readTree(
        "{\"email\":\"ios@test.com\",\"name\":\"iosCap\",\"picture\":\"ios.png\"}");
    when(restTemplate.exchange(eq("http://capstone-resource-uri"), eq(HttpMethod.GET),
        any(HttpEntity.class), eq(JsonNode.class)))
        .thenReturn(new ResponseEntity<>(userInfoResponse, HttpStatus.OK));

    when(userRepository.findByEmail("ios@test.com")).thenReturn(Optional.of(existingUser));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    TokenDto result = googleService.loginOrJoin(
        "ios-auth-code", "com.googleusercontent.apps.test:/oauth", true);

    assertNotNull(result);
    assertEquals("access", result.getAccessToken());
    assertEquals("iosCap", result.getName());
  }

  @Test
  @DisplayName("실패 - 토큰 URI 호출 오류")
  void failTokenURILoginOrJoin() {
    when(restTemplate.exchange(
        anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(JsonNode.class)))
        .thenThrow(new RuntimeException("capstone-token-uri 실패"));

    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> googleService.loginOrJoin("code", "http://localhost/callback", false));
    assertEquals("capstone-token-uri 실패", ex.getMessage());
  }

  @Test
  @DisplayName("실패 - 사용자 정보 URI 호출 오류")
  void failUserURILoginOrJoin() throws Exception {
    JsonNode tokenResponse = objectMapper.readTree("{\"access_token\":\"mock-access-token\"}");
    when(restTemplate.exchange(eq("http://capstone-token-uri"), eq(HttpMethod.POST),
        any(HttpEntity.class), eq(JsonNode.class)))
        .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

    when(restTemplate.exchange(eq("http://capstone-resource-uri"), eq(HttpMethod.GET),
        any(HttpEntity.class), eq(JsonNode.class)))
        .thenThrow(new RuntimeException("capstone-resource-uri 실패"));

    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> googleService.loginOrJoin("code", "http://localhost/callback", false));
    assertEquals("capstone-resource-uri 실패", ex.getMessage());
  }

  @Test
  @DisplayName("실패 - 중복 저장 후 재조회도 실패하면 예외 발생")
  void failDuplicateLoginOrJoin() throws Exception {
    JsonNode tokenResponse = objectMapper.readTree("{\"access_token\":\"mock-access-token\"}");
    when(restTemplate.exchange(eq("http://capstone-token-uri"), eq(HttpMethod.POST),
        any(HttpEntity.class), eq(JsonNode.class)))
        .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

    JsonNode userInfoResponse = objectMapper.readTree(
        "{\"email\":\"fail@test.com\",\"name\":\"failuser\",\"picture\":null}");
    when(restTemplate.exchange(eq("http://capstone-resource-uri"), eq(HttpMethod.GET),
        any(HttpEntity.class), eq(JsonNode.class)))
        .thenReturn(new ResponseEntity<>(userInfoResponse, HttpStatus.OK));

    when(userRepository.findByEmail("fail@test.com"))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.empty());

    when(userRepository.save(any(User.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate"));

    assertThrows(RuntimeException.class,
        () -> googleService.loginOrJoin("code", "http://localhost/callback", false));
  }
}