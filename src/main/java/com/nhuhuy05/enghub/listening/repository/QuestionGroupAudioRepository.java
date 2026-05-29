package com.nhuhuy05.enghub.listening.repository;

import com.nhuhuy05.enghub.listening.entity.QuestionGroupAudio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface QuestionGroupAudioRepository extends JpaRepository<QuestionGroupAudio, Long> {
    Optional<QuestionGroupAudio> findByQuestionGroupIdAndOrderIndex(Long questionGroupId, Integer orderIndex);

    boolean existsByMediaAssetId(Long mediaAssetId);

    @Query("""
            select count(qg)
            from QuestionGroup qg
            where qg.testPart.test.id = :testId
              and qg.testPart.partNumber between 1 and 4
              and not exists (
                  select 1
                  from QuestionGroupAudio r
                  where r.questionGroup = qg
                    and r.startMs >= 0
                    and (r.endMs is null or r.endMs > r.startMs)
              )
            """)
    long countListeningGroupsWithoutValidAudioRange(Long testId);
}
