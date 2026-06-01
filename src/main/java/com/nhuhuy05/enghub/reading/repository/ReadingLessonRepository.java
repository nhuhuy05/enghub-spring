package com.nhuhuy05.enghub.reading.repository;

import com.nhuhuy05.enghub.reading.entity.ReadingLesson;
import com.nhuhuy05.enghub.reading.enums.ReadingLessonStatus;
import com.nhuhuy05.enghub.reading.enums.ReadingLessonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingLessonRepository extends JpaRepository<ReadingLesson, Long> {
    Optional<ReadingLesson> findByQuestionGroupId(Long questionGroupId);

    boolean existsByQuestionGroupId(Long questionGroupId);

    List<ReadingLesson> findAllByOrderByUpdatedAtDesc();

    List<ReadingLesson> findAllByStatusOrderByUpdatedAtDesc(ReadingLessonStatus status);

    List<ReadingLesson> findAllByReadingTypeOrderByUpdatedAtDesc(ReadingLessonType readingType);

    List<ReadingLesson> findAllByStatusAndReadingTypeOrderByUpdatedAtDesc(
            ReadingLessonStatus status,
            ReadingLessonType readingType
    );

}
