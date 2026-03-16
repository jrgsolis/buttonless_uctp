package com.demo.uctp.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("error", "bad_request");
    body.put("message", ex.getMessage());
    body.put("timestamp", OffsetDateTime.now().toString());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }
}