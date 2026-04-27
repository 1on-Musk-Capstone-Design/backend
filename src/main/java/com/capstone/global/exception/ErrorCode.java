package com.capstone.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  NOT_FOUND_WORKSPACE("워크스페이스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  NOT_FOUND_USER("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  NOT_FOUND_CANVAS("캔버스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  NOT_FOUND_IDEA("아이디어를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
  NOT_FOUND_SESSION("해당 음성 세션을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
  NOT_FOUND_SESSION_USER("해당 세션에서 사용자를 찾을 수 없거나 이미 퇴장했습니다.", HttpStatus.NOT_FOUND),
  NOT_FOUND_WORKSPACE_USER("해당 워크스페이스 유저를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

  ALREADY_JOINED_WORKSPACE("이미 이 워크스페이스에 참여 중입니다.", HttpStatus.BAD_REQUEST),
  ALREADY_JOINED_SESSION("이미 이 세션에 참여 중입니다.", HttpStatus.BAD_REQUEST),

  FORBIDDEN_WORKSPACE("워크스페이스 권한이 없습니다.", HttpStatus.FORBIDDEN),
  FORBIDDEN_WORKSPACE_SESSION("해당 세션은 해당 워크스페이스에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN),
  FORBIDDEN_WORKSPACE_ACCESS("워크스페이스 소속 사용자가 아닙니다.", HttpStatus.FORBIDDEN),
  FORBIDDEN_CLOSED_SESSION("이미 종료된 세션에는 참여할 수 없습니다.", HttpStatus.FORBIDDEN),

  APPLE_AUTH_FAILED("Apple 인증 처리 중 오류가 발생했습니다.", HttpStatus.UNAUTHORIZED),
  UNAUTHORIZED_USER("인증 정보가 필요합니다.", HttpStatus.UNAUTHORIZED),
  INVALID_TOKEN("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),

  INVALID_INVITE_TOKEN("유효하지 않은 초대 토큰입니다.", HttpStatus.BAD_REQUEST),
  EXPIRED_INVITE_TOKEN("만료된 초대 토큰입니다.", HttpStatus.BAD_REQUEST),
  BAD_REQUEST("잘못된 요청입니다.", HttpStatus.BAD_REQUEST),

  NOT_FOUND_PROTOTYPE_JOB("해당 아이디어에 대한 프로토타입 작업이 없습니다.", HttpStatus.NOT_FOUND),
  NOT_FOUND_PROTOTYPE_JOB_BY_ID("해당 프로토타입 작업을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  NOT_FOUND_PROTOTYPE_ARTIFACT("저장된 프로토타입 소스를 찾을 수 없습니다. 파이프라인이 끝난 뒤 다시 시도하세요.", HttpStatus.NOT_FOUND),
  PROTOTYPE_ARTIFACT_IO_FAILED("프로토타입 파일을 읽거나 쓰는 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  PROTOTYPE_PIPELINE_FAILED("프로토타입 파이프라인 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

  private final String message;
  private final HttpStatus httpStatus;
}
