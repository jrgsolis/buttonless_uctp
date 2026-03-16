package com.demo.uctp.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HttpSignatureUtil {

  private HttpSignatureUtil() {}

  public static String digestSha256Base64(byte[] bodyBytes) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(bodyBytes);
      return Base64.getEncoder().encodeToString(hash);
    } catch (Exception e) {
      throw new RuntimeException("Digest error: " + e.getMessage(), e);
    }
  }

  public static String hmacSha256Base64(byte[] secretKeyBytes, String data) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secretKeyBytes, "HmacSHA256"));
      byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(raw);
    } catch (Exception e) {
      throw new RuntimeException("HMAC error: " + e.getMessage(), e);
    }
  }
}
