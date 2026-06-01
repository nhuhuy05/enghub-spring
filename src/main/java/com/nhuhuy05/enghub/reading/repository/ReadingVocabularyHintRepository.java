package com.nhuhuy05.enghub.reading.repository;

import com.nhuhuy05.enghub.reading.entity.ReadingVocabularyHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReadingVocabularyHintRepository extends JpaRepository<ReadingVocabularyHint, Long> {
    List<ReadingVocabularyHint> findAllByReadingLessonIdOrderByOrderIndexAscIdAsc(Long readingLessonId);

    void deleteAllByReadingLessonId(Long readingLessonId);
}
