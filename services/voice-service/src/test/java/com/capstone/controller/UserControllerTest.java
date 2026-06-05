package com.capstone.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capstone.domain.user.UserController;
import com.capstone.domain.user.UserResponse;
import com.capstone.domain.user.UserService;
import com.capstone.domain.user.UserUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private UserService userService;

  private final String VALID_TOKEN = "Bearer 테스트 토큰";
  private final String RAW_TOKEN = "테스트 토큰";

  @Test
  @DisplayName(" 올바른 JWT 토큰으로 조회 시 사용자 정보를 반환 성공")
  void successGet() throws Exception {
    UserResponse expectedResponse = UserResponse.builder()
        .id(1L)
        .email("테스트 메일")
        .name("테스트 이름")
        .profileImage("사진")
        .build();

    given(userService.getCurrentUser(RAW_TOKEN)).willReturn(expectedResponse);

    mockMvc.perform(get("/v1/users/me")
            .header(HttpHeaders.AUTHORIZATION, VALID_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.email").value("테스트 메일"))
        .andExpect(jsonPath("$.name").value("테스트 이름"))
        .andExpect(jsonPath("$.profileImage").value("사진"))
        .andDo(print());
  }

  @Test
  @DisplayName("토큰 헤더 형식이 올바르지 않으면 401 에러 - 실패")
  void failGet() throws Exception {
    String INVALID_TOKEN = "Invalid 토큰";
    mockMvc.perform(get("/v1/users/me")
            .header(HttpHeaders.AUTHORIZATION, INVALID_TOKEN))
        .andExpect(status().isUnauthorized())
        .andDo(print());
  }


  @Test
  @DisplayName("요청 데이터가 올바르면 사용자 정보 수정 성공")
  void successUpdate() throws Exception {
    UserUpdateRequest request = new UserUpdateRequest();
    request.setName("수정된이름");
    request.setProfileImage("새로운 사진");

    UserResponse expectedResponse = UserResponse.builder()
        .id(1L)
        .email("테스트 메일")
        .name("수정된이름")
        .profileImage("새로운 사진")
        .build();

    given(userService.updateUser(eq(RAW_TOKEN), any(UserUpdateRequest.class)))
        .willReturn(expectedResponse);

    mockMvc.perform(patch("/v1/users/me")
            .header(HttpHeaders.AUTHORIZATION, VALID_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("수정된이름"))
        .andExpect(jsonPath("$.profileImage").value("새로운 사진"))
        .andDo(print());
  }

  @Test
  @DisplayName("토큰 헤더 없이 수정을 요청하면 401 에러 - 실패")
  void failUpdate() throws Exception {
    UserUpdateRequest request = new UserUpdateRequest();
    request.setName("새로운 이름");

    mockMvc.perform(patch("/v1/users/me")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andDo(print());
  }


  @Test
  @DisplayName("회원 탈퇴 처리가 완료되면 204 No Content를 반환 성공")
  void successDelete() throws Exception {
    doNothing().when(userService).deleteUser(RAW_TOKEN);

    mockMvc.perform(delete("/v1/users/me")
            .header(HttpHeaders.AUTHORIZATION, VALID_TOKEN))
        .andExpect(status().isNoContent())
        .andDo(print());
  }

  @Test
  @DisplayName("토큰 헤더가 누락되면 401 에러 - 실패")
  void failDelete() throws Exception {
    mockMvc.perform(delete("/v1/users/me"))
        .andExpect(status().isUnauthorized())
        .andDo(print());
  }
}