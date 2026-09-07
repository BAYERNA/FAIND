package com.faind.domain.auth.repository;

import com.faind.domain.auth.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

  Optional<User> findByBadgeNumber(String badgeNumber);

  boolean existsByBadgeNumber(String badgeNumber);

  long countByRoleAndStatus(String role, String status);
}
