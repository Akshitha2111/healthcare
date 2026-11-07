package com.healthcare.repository;

import com.healthcare.entity.Role;
import com.healthcare.entity.User;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(Role role);
    long countByRoleAndCreatedAtAfter(Role role, Instant since);
}
