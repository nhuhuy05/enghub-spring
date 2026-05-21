package com.nhuhuy05.enghub.test.repository;

import com.nhuhuy05.enghub.common.enums.AttemptStatus;
import com.nhuhuy05.enghub.test.entity.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {
    List<TestAttempt> findByUserIdAndStatusOrderBySubmittedAtDesc(Long userId, AttemptStatus status);
    java.util.Optional<TestAttempt> findByIdAndUserId(Long id, Long userId);
    boolean existsByTestId(Long testId);
}
