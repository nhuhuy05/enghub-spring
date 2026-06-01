package com.nhuhuy05.enghub.vocabulary.repository;

import com.nhuhuy05.enghub.vocabulary.entity.UserVocabularyProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserVocabularyProgressRepository extends JpaRepository<UserVocabularyProgress, Long> {
    Optional<UserVocabularyProgress> findByUserIdAndVocabularyId(Long userId, Long vocabularyId);

    List<UserVocabularyProgress> findAllByUserIdOrderByLearnedAtDesc(Long userId);

    List<UserVocabularyProgress> findAllByUserIdAndVocabularyIdIn(Long userId, List<Long> vocabularyIds);

    @Query("""
            select progress
            from UserVocabularyProgress progress
            where progress.user.id = :userId
              and progress.mastered = false
              and progress.nextReviewAt is not null
              and progress.nextReviewAt <= :now
              and (:topicId is null or exists (
                  select 1
                  from VocabularyTopicMap topicMap
                  where topicMap.vocabulary = progress.vocabulary
                    and topicMap.topic.id = :topicId
              ))
            order by progress.nextReviewAt asc, progress.vocabulary.word asc
            """)
    List<UserVocabularyProgress> findDue(Long userId, Long topicId, LocalDateTime now);
}
