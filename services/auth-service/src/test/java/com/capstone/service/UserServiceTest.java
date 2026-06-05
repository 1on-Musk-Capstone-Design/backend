package com.capstone.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.capstone.domain.user.User;
import com.capstone.domain.user.UserRepository;
import com.capstone.domain.user.UserResponse;
import com.capstone.domain.user.UserService;
import com.capstone.domain.user.UserUpdateRequest;
import com.capstone.domain.workspace.WorkspaceRepository;
import com.capstone.domain.workspaceInvitation.WorkspaceInvitationRepository;
import com.capstone.domain.workspaceInvite.WorkspaceInviteRepository;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import com.capstone.global.exception.CustomException;
import com.capstone.global.exception.ErrorCode;
import com.capstone.global.oauth.JwtProvider;
import io.jsonwebtoken.JwtException;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  @InjectMocks
  private UserService userService;

  @Mock
  private UserRepository userRepository;
  @Mock
  private WorkspaceUserRepository workspaceUserRepository;
  @Mock
  private WorkspaceInviteRepository workspaceInviteRepository;
  @Mock
  private WorkspaceInvitationRepository workspaceInvitationRepository;
  @Mock
  private WorkspaceRepository workspaceRepository;

  @Mock
  private JwtProvider jwtProvider;

  private final String RAW_TOKEN = "테스트 토큰";
  private final Long USER_ID = 1L;

  @Test
  @DisplayName("현재 사용자 정보 조회 성공")
  void successGet() {
    User user = User.builder()
        .id(USER_ID)
        .email("테스트 메일")
        .name("테스트 이름")
        .profileImage("테스트 사진")
        .build();

    given(jwtProvider.getUserIdFromAccessToken(RAW_TOKEN)).willReturn(USER_ID);
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

    UserResponse response = userService.getCurrentUser(RAW_TOKEN);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(USER_ID);
    assertThat(response.getName()).isEqualTo("테스트 이름");
  }

  @Test
  @DisplayName("현재 사용자 정보 조회 실패 - 유효하지 않은 토큰")
  void failGet() {
    given(jwtProvider.getUserIdFromAccessToken(RAW_TOKEN)).willThrow(new JwtException("만료된 토큰"));

    assertThatThrownBy(() -> userService.getCurrentUser(RAW_TOKEN))
        .isInstanceOf(CustomException.class)
        .hasMessageContaining(ErrorCode.INVALID_TOKEN.getMessage());
  }

  @Test
  @DisplayName("사용자 정보 수정 성공")
  void successUpdate() {
    User user = User.builder()
        .id(USER_ID)
        .name("이전 이름")
        .profileImage("테스트 사진")
        .build();

    UserUpdateRequest request = new UserUpdateRequest();
    request.setName("수정된 이름");
    request.setProfileImage("새로운 사진");

    given(jwtProvider.getUserIdFromAccessToken(RAW_TOKEN)).willReturn(USER_ID);
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
    given(userRepository.saveAndFlush(any(User.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    UserResponse response = userService.updateUser(RAW_TOKEN, request);

    assertThat(response.getName()).isEqualTo("수정된 이름");
    assertThat(response.getProfileImage()).isEqualTo("새로운 사진");
  }

  @Test
  @DisplayName("사용자 정보 수정 실패 - 존재하지 않는 유저")
  void failUpdate() {
    UserUpdateRequest request = new UserUpdateRequest();
    request.setName("수정 이름");

    given(jwtProvider.getUserIdFromAccessToken(RAW_TOKEN)).willReturn(USER_ID);
    given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

    assertThatThrownBy(() -> userService.updateUser(RAW_TOKEN, request))
        .isInstanceOf(CustomException.class)
        .hasMessageContaining(ErrorCode.NOT_FOUND_USER.getMessage());
  }

  @Test
  @DisplayName("회원 탈퇴 처리 성공")
  void successDelete() {
    User user = User.builder()
        .id(USER_ID)
        .refreshToken("존재 토큰")
        .build();

    given(jwtProvider.getUserIdFromAccessToken(RAW_TOKEN)).willReturn(USER_ID);
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

    given(workspaceInvitationRepository.findByInvitedUserOrInvitedBy(user, user)).willReturn(
        new ArrayList<>());
    given(workspaceInviteRepository.findByCreatedBy(user)).willReturn(new ArrayList<>());
    given(workspaceRepository.findByOwner(user)).willReturn(new ArrayList<>());
    given(workspaceUserRepository.findByUser(user)).willReturn(new ArrayList<>());

    userService.deleteUser(RAW_TOKEN);

    assertThat(user.getRefreshToken()).isNull();
    verify(userRepository).delete(user);
  }

  @Test
  @DisplayName("회원 탈퇴 처리 실패 - 원본 예외가 그대로 던져짐")
  void failDelete() {
    given(jwtProvider.getUserIdFromAccessToken(RAW_TOKEN)).willReturn(USER_ID);
    given(userRepository.findById(USER_ID)).willThrow(new RuntimeException("DB 연결 실패"));

    assertThatThrownBy(() -> userService.deleteUser(RAW_TOKEN))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("DB 연결 실패");
  }
}