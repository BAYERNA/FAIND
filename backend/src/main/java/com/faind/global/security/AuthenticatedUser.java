package com.faind.global.security;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String role) {

  public boolean isAdmin() {
    return "ADMIN".equals(role);
  }
}
