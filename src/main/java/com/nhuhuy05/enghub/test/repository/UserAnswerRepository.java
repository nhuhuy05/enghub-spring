package com.nhuhuy05.enghub.test.repository;

import com.nhuhuy05.enghub.test.entity.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {
    Optional<UserAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);
    List<UserAnswer> findAllByAttemptId(Long attemptId);
    List<UserAnswer> findAllByAttemptIdAndQuestionIdIn(Long attemptId, List<Long> questionIds);
}
