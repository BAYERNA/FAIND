package com.faind.global.security;

import com.faind.domain.auth.entity.User;
import com.faind.domain.auth.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  public CustomUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public CustomUserDetails loadUserByUsername(String badgeNumber) throws UsernameNotFoundException {
    User user = userRepository.findByBadgeNumber(badgeNumber)
        .orElseThrow(() -> new UsernameNotFoundException("badge_number=" + badgeNumber));
    return new CustomUserDetails(user);
  }
}
