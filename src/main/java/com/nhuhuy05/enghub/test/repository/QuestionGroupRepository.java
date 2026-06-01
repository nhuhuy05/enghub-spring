package com.nhuhuy05.enghub.test.repository;

import com.nhuhuy05.enghub.test.entity.QuestionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    @Query("""
            select qg
            from QuestionGroup qg
            where qg.testPart.test.id = :testId
              and qg.testPart.partNumber in :partNumbers
            order by qg.testPart.partNumber asc, qg.orderIndex asc
            """)
    List<QuestionGroup> findAllByTestIdAndPartNumbersOrderByPartAndOrder(Long testId, List<Integer> partNumbers);

    @Query("""
            select qg
            from QuestionGroup qg
            where qg.testPart.partNumber = 7
              and (:testId is null or qg.testPart.test.id = :testId)
            order by qg.testPart.test.createdAt desc, qg.orderIndex asc
            """)
    List<QuestionGroup> findAllPart7Candidates(Long testId);

    long countByTestPartTestIdAndReviewStatusNot(Long testId, String reviewStatus);
}
