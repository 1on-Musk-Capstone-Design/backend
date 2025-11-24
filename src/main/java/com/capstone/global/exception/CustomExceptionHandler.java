package com.capstone.global.exception;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CustomExceptionHandler {

  @ExceptionHandler(CustomException.class)
  public ResponseEntity<String> customExceptionHandler(final CustomException e) {

    return ResponseEntity.status(e.getErrorCode().getHttpStatus())
        .body(e.getErrorCode().getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<String> processValidationError(MethodArgumentNotValidException e) {

    String errorMessage = e.getBindingResult().getFieldErrors()
        .stream().map(DefaultMessageSourceResolvable::getDefaultMessage).toList().getFirst();

    return ResponseEntity.badRequest().body(errorMessage);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<String> runtimeExceptionHandler(final RuntimeException e) {
    return ResponseEntity.status(500).body("서버 오류가 발생했습니다.");
  }
}
