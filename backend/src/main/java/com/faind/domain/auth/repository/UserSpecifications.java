package com.faind.domain.auth.repository;

import com.faind.domain.auth.entity.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

// NFR-05: 대원 계정 목록의 검색·필터는 MVP 필수 — QA에서 반복 미작동했던 지점이라
// 쿼리 파라미터가 실제로 WHERE 절까지 바인딩되는지가 이 클래스의 존재 이유다.
public final class UserSpecifications {

  private UserSpecifications() {}

  public static Specification<User> search(String keyword, String role) {
    return (root, query, cb) -> {
      var predicates = cb.conjunction();
      if (StringUtils.hasText(keyword)) {
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        predicates = cb.and(
            predicates,
            cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("badgeNumber")), pattern),
                cb.like(cb.lower(root.get("team")), pattern)));
      }
      if (StringUtils.hasText(role) && !"ALL".equalsIgnoreCase(role)) {
        predicates = cb.and(predicates, cb.equal(root.get("role"), role));
      }
      predicates = cb.and(predicates, cb.notEqual(root.get("status"), "INACTIVE"));
      return predicates;
    };
  }
}
