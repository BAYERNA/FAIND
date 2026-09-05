package com.faind.domain.auth.service;

import com.faind.domain.auth.dto.LoginRequest;
import com.faind.domain.auth.dto.LoginResponse;
import com.faind.domain.auth.dto.SetInitialPasswordRequest;
import com.faind.domain.auth.entity.User;
import com.faind.domain.auth.repository.UserRepository;
import com.faind.global.error.BusinessException;
import com.faind.global.error.ErrorCode;
import com.faind.global.security.JwtTokenProvider;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// FR-01 통합 로그인 + CMN-002 최초 비밀번호 설정.
@Service
@Transactional(readOnly = true)
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  public LoginResponse login(LoginRequest request) {
    User user = userRepository.findByBadgeNumber(request.badgeNumber())
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

    if (!"ACTIVE".equals(user.getStatus()) || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
    }

    String accessToken = jwtTokenProvider.createToken(user.getUserId(), user.getBadgeNumber(), user.getRole());
    return new LoginResponse(accessToken, user.getUserId(), user.getName(), user.getRole(), user.isInitialPassword());
  }

  @Transactional
  public void setInitialPassword(UUID userId, SetInitialPasswordRequest request) {
    if (!request.isConfirmed()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "새 비밀번호와 확인 값이 일치하지 않습니다.");
    }
    User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    user.completeInitialPasswordSetup(passwordEncoder.encode(request.newPassword()));
  }
}
