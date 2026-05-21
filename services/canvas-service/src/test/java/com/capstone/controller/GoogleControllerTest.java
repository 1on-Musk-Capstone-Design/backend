package com.capstone.controller;

import com.capstone.global.config.AppProperties;
import com.capstone.global.oauth.TokenDto;
import com.capstone.global.oauth.controller.GoogleController;
import com.capstone.global.oauth.service.GoogleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GoogleController.class)
@AutoConfigureMockMvc(addFilters = false)
class GoogleControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private GoogleService googleService;

  @MockBean
  private AppProperties appProperties;

  @Test
  @DisplayName("웹 로그인 성공 - redirect_uri 파라미터 직접 전달")
  void successLoginOrJoin() throws Exception {
    TokenDto tokenDto = TokenDto.builder()
        .accessToken("access")
        .refreshToken("refresh")
        .name("cap")
        .email("capstone@test.com")
        .build();

    given(googleService.loginOrJoin(anyString(), anyString(), eq(false))).willReturn(tokenDto);

    mockMvc.perform(post("/v1/auth-google")
            .param("code", "auth-code")
            .param("redirect_uri", "http://localhost:3000/auth/callback")
            .param("platform", "web")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("access"))
        .andExpect(jsonPath("$.refreshToken").value("refresh"))
        .andExpect(jsonPath("$.name").value("cap"))
        .andExpect(jsonPath("$.email").value("capstone@test.com"));
  }

  @Test
  @DisplayName("iOS 로그인 성공 - platform=ios")
  void successIosLoginOrJoin() throws Exception {
    TokenDto tokenDto = TokenDto.builder()
        .accessToken("ios-access")
        .refreshToken("ios-refresh")
        .name("ioscap")
        .email("capstone@test.com")
        .build();

    given(googleService.loginOrJoin(anyString(), anyString(), eq(true))).willReturn(tokenDto);

    mockMvc.perform(post("/v1/auth-google")
            .param("code", "ios-auth-code")
            .param("redirect_uri", "com.googleusercontent.apps.test:/oauth")
            .param("platform", "ios")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("ios-access"))
        .andExpect(jsonPath("$.refreshToken").value("ios-refresh"));
  }

  @Test
  @DisplayName("로그인 실패 - 구글 인증 오류로 5xx 반환")
  void failLoginOrJoin() throws Exception {
    given(appProperties.getOauthRedirectMap()).willReturn(Map.of());
    given(appProperties.getDefaultRedirectUri()).willReturn("http://localhost:3000/auth/callback");
    given(googleService.loginOrJoin(anyString(), anyString(), anyBoolean()))
        .willThrow(new RuntimeException("구글 인증 실패"));

    mockMvc.perform(post("/v1/auth-google")
            .param("code", "wrong-code")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().is5xxServerError());
  }


  @Test
  @DisplayName("웹 로그인 URI 조회 성공")
  void successGetWebLoginUri() throws Exception {
    mockMvc.perform(get("/v1/auth-google/login-uri")
            .param("redirect_uri", "http://localhost:3000/auth/callback")
            .param("platform", "web"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("accounts.google.com")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("redirect_uri")));
  }

  @Test
  @DisplayName("iOS 로그인 URI 조회 성공")
  void successGetIosLoginUri() throws Exception {
    mockMvc.perform(get("/v1/auth-google/login-uri")
            .param("redirect_uri", "com.googleusercontent.apps.test:/oauth")
            .param("platform", "ios"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("accounts.google.com")));
  }

  @Test
  @DisplayName("로그인 URI 조회 실패 - iOS인데 웹 형식 redirect_uri 전달")
  void failGetIosLoginUri() throws Exception {
    mockMvc.perform(get("/v1/auth-google/login-uri")
            .param("redirect_uri", "http://localhost:3000/auth/callback")
            .param("platform", "ios"))
        .andExpect(status().is5xxServerError());
  }

  @Test
  @DisplayName("로그인 URI 조회 실패 - 웹인데 iOS 형식 redirect_uri 전달")
  void failGetWebLoginUri() throws Exception {
    mockMvc.perform(get("/v1/auth-google/login-uri")
            .param("redirect_uri", "com.googleusercontent.apps.test:/oauth")
            .param("platform", "web"))
        .andExpect(status().is5xxServerError());
  }
}