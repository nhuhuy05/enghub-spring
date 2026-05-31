package com.nhuhuy05.enghub.test.repository;

import com.nhuhuy05.enghub.common.enums.AttemptMode;
import com.nhuhuy05.enghub.common.enums.AttemptStatus;
import com.nhuhuy05.enghub.test.entity.TestAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {
    List<TestAttempt> findByUserIdAndStatusOrderBySubmittedAtDesc(Long userId, AttemptStatus status);
    Optional<TestAttempt> findByIdAndUserId(Long id, Long userId);

    @Query("""
            select attempt
            from TestAttempt attempt
            where attempt.user.id = :userId
              and (:status is null or attempt.status = :status)
              and (:testId is null or attempt.test.id = :testId)
            order by attempt.startedAt desc
            """)
    Page<TestAttempt> findAttempts(Long userId, AttemptStatus status, Long testId, Pageable pageable);

    Optional<TestAttempt> findFirstByUserIdAndTestIdAndModeAndStatusAndSelectedPartNumbersOrderByStartedAtDesc(
            Long userId,
            Long testId,
            AttemptMode mode,
            AttemptStatus status,
            String selectedPartNumbers
    );
    boolean existsByTestId(Long testId);
}
