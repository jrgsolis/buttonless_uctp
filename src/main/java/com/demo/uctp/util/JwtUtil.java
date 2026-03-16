package com.demo.uctp.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Map;

public final class JwtUtil {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private JwtUtil() {}

  @SuppressWarnings("unchecked")
  public static Map<String, Object> decodeJwtPayload(String jwt) {
    try {
      String[] parts = jwt.split("\\.");
      if (parts.length < 2) throw new IllegalArgumentException("Invalid JWT format");
      byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
      return MAPPER.readValue(payloadBytes, Map.class);
    } catch (Exception e) {
      throw new RuntimeException("Failed to decode JWT payload: " + e.getMessage(), e);
    }
  }
}
