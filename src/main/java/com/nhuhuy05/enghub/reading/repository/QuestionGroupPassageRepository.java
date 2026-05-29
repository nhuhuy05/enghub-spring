package com.nhuhuy05.enghub.reading.repository;

import com.nhuhuy05.enghub.reading.entity.QuestionGroupPassage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface QuestionGroupPassageRepository extends JpaRepository<QuestionGroupPassage, Long> {
    Optional<QuestionGroupPassage> findByQuestionGroupIdAndOrderIndex(Long questionGroupId, Integer orderIndex);

    boolean existsByMediaAssetId(Long mediaAssetId);

    @Query("""
            select count(qg)
            from QuestionGroup qg
            where qg.testPart.test.id = :testId
              and qg.testPart.partNumber in (6, 7)
              and not exists (
                  select 1
                  from QuestionGroupPassage p
                  where p.questionGroup = qg
              )
            """)
    long countReadingGroupsWithoutPassage(Long testId);
}
