package com.capstone.controller;

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

@WebMvcTest(GoogleController.class)
@AutoConfigureMockMvc(addFilters = false)
class GoogleControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private GoogleService googleService;

  @Test
  @DisplayName("로그인 & 회원가입 성공 테스트")
  void successLoginOrJoin() throws Exception {

    TokenDto tokenDto = TokenDto.builder()
        .accessToken("access")
        .refreshToken("refresh")
        .build();

    given(googleService.loginOrJoin("Wrong Code")).willReturn(tokenDto);

    mockMvc.perform(post("/api/v1/auth-google")
            .param("code", "Wrong Code")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("access"))
        .andExpect(jsonPath("$.refreshToken").value("refresh"));
  }


  @Test
  @DisplayName("로그인 & 회원가입 실패 테스트")
  void failLoginOrJoin() {
    when(googleService.loginOrJoin(anyString()))
        .thenThrow(new RuntimeException("구글 인증 실패"));

    Throwable exception = assertThrows(Exception.class,
        () -> mockMvc.perform(post("/api/v1/auth-google")
                .param("code", "Wrong Code"))
            .andReturn());

    assertEquals("구글 인증 실패", exception.getCause().getMessage());

  }
}