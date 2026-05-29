package com.nhuhuy05.enghub.test.repository;

import com.nhuhuy05.enghub.test.entity.QuestionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface QuestionGroupRepository extends JpaRepository<QuestionGroup, Long> {
    Optional<QuestionGroup> findByTestPartTestIdAndTestPartPartNumberAndOrderIndex(
            Long testId,
            Integer partNumber,
            Integer orderIndex
    );

    List<QuestionGroup> findAllByTestPartTestId(Long testId);

    List<QuestionGroup> findAllByTestPartTestIdOrderByTestPartPartNumberAscOrderIndexAsc(Long testId);

    long countByTestPartTestIdAndReviewStatusNot(Long testId, String reviewStatus);
}
