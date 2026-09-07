package com.faind.domain.auth.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
class TemporaryPasswordGenerator {

  private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
  private static final SecureRandom RANDOM = new SecureRandom();

  String generate() {
    StringBuilder sb = new StringBuilder(12);
    for (int i = 0; i < 12; i++) {
      sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
    }
    return sb.toString();
  }
}
