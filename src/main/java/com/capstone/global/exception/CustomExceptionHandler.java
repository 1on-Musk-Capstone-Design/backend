package com.capstone.global.exception;

import io.jsonwebtoken.JwtException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class CustomExceptionHandler {

  @ExceptionHandler(CustomException.class)
  public ResponseEntity<String> customExceptionHandler(final CustomException e) {

    return ResponseEntity.status(e.getErrorCode().getHttpStatus())
        .body(e.getErrorCode().getMessage());
  }

  @ExceptionHandler(JwtException.class)
  public ResponseEntity<String> jwtExceptionHandler(final JwtException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 토큰입니다.");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<String> dataIntegrityHandler(final DataIntegrityViolationException e) {
    String root = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body("데이터 저장 제약 위반입니다. (예: 필드 길이 초과) " + (root != null ? root : ""));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<String> processValidationError(MethodArgumentNotValidException e) {

    String errorMessage = e.getBindingResult().getFieldErrors()
        .stream().map(DefaultMessageSourceResolvable::getDefaultMessage).toList().getFirst();

    return ResponseEntity.badRequest().body(errorMessage);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<String> runtimeExceptionHandler(final RuntimeException e) {
    log.error("처리되지 않은 서버 오류", e);
    return ResponseEntity.status(500).body("서버 오류가 발생했습니다.");
  }
}
