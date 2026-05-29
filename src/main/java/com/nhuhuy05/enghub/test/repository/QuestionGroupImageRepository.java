package com.nhuhuy05.enghub.test.repository;

import com.nhuhuy05.enghub.test.entity.QuestionGroupImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionGroupImageRepository extends JpaRepository<QuestionGroupImage, Long> {
    Optional<QuestionGroupImage> findByQuestionGroupIdAndOrderIndex(Long questionGroupId, Integer orderIndex);

    boolean existsByMediaAssetId(Long mediaAssetId);

    @Query("""
            select count(qg)
            from QuestionGroup qg
            where qg.testPart.test.id = :testId
              and qg.testPart.partNumber = 1
              and not exists (
                  select 1
                  from QuestionGroupImage image
                  where image.questionGroup = qg
              )
            """)
    long countPartOneGroupsWithoutImage(Long testId);
}
