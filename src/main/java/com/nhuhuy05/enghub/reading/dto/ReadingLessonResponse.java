package com.nhuhuy05.enghub.reading.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nhuhuy05.enghub.reading.enums.ReadingLessonStatus;
import com.nhuhuy05.enghub.reading.enums.ReadingLessonType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingLessonResponse {
    Long id;

    @JsonProperty("question_group_id")
    Long questionGroupId;

    @JsonProperty("test_id")
    Long testId;

    @JsonProperty("test_title")
    String testTitle;

    @JsonProperty("group_order")
    Integer groupOrder;

    String title;

    @JsonProperty("title_vi")
    String titleVi;

    @JsonProperty("reading_type")
    ReadingLessonType readingType;

    ReadingLessonStatus status;
    String difficulty;

    List<PassageResponse> passages;

    @JsonProperty("vocabulary_hints")
    List<ReadingVocabularyHintResponse> vocabularyHints;

    @JsonProperty("created_at")
    LocalDateTime createdAt;

    @JsonProperty("updated_at")
    LocalDateTime updatedAt;
}
