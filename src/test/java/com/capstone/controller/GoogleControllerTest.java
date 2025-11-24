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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;

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
  @DisplayName("로그인 & 회원가입 성공 테스트")
  void successLoginOrJoin() throws Exception {

    TokenDto tokenDto = TokenDto.builder()
        .accessToken("access")
        .refreshToken("refresh")
        .build();

    given(appProperties.getOauthRedirectMap()).willReturn(Map.of("localhost", "http://localhost:3000/auth/callback"));
    given(appProperties.getDefaultRedirectUri()).willReturn("http://localhost:3000/auth/callback");
    given(googleService.loginOrJoin(anyString(), anyString())).willReturn(tokenDto);

    mockMvc.perform(post("/v1/auth-google")
            .param("code", "Wrong Code")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("access"))
        .andExpect(jsonPath("$.refreshToken").value("refresh"));
  }


  @Test
  @DisplayName("로그인 & 회원가입 실패 테스트")
  void failLoginOrJoin() throws Exception {
    when(appProperties.getOauthRedirectMap()).thenReturn(Map.of());
    when(appProperties.getDefaultRedirectUri()).thenReturn("http://localhost:3000/auth/callback");
    when(googleService.loginOrJoin(anyString(), anyString()))
        .thenThrow(new RuntimeException("구글 인증 실패"));

    mockMvc.perform(post("/v1/auth-google")
            .param("code", "Wrong Code")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().is5xxServerError());
  }
}