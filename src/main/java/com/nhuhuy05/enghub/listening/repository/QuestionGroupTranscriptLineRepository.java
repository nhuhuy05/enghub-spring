package com.nhuhuy05.enghub.listening.repository;

import com.nhuhuy05.enghub.listening.entity.QuestionGroupTranscriptLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionGroupTranscriptLineRepository extends JpaRepository<QuestionGroupTranscriptLine, Long> {
    List<QuestionGroupTranscriptLine> findAllByQuestionGroupAudioIdOrderByOrderIndexAsc(Long audioId);

    boolean existsByQuestionGroupAudioId(Long audioId);
}
