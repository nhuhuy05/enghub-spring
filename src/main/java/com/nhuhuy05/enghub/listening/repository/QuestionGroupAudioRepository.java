package com.nhuhuy05.enghub.listening.repository;

import com.nhuhuy05.enghub.listening.entity.QuestionGroupAudioRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface QuestionGroupAudioRangeRepository extends JpaRepository<QuestionGroupAudioRange, Long> {
    Optional<QuestionGroupAudioRange> findByQuestionGroupIdAndOrderIndex(Long questionGroupId, Integer orderIndex);

    boolean existsByMediaAssetId(Long mediaAssetId);

    @Query("""
            select count(qg)
            from QuestionGroup qg
            where qg.testPart.test.id = :testId
              and qg.testPart.partNumber between 1 and 4
              and not exists (
                  select 1
                  from QuestionGroupAudioRange r
                  where r.questionGroup = qg
                    and r.startMs >= 0
                    and (r.endMs is null or r.endMs > r.startMs)
              )
            """)
    long countListeningGroupsWithoutValidAudioRange(Long testId);
}
