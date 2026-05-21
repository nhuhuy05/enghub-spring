package com.nhuhuy05.enghub.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestPreviewResponse {
    @JsonProperty("test_id")
    Long testId;

    @JsonProperty("question_count")
    Long questionCount;

    @JsonProperty("invalid_correct_answer_count")
    Long invalidCorrectAnswerCount;

    @JsonProperty("part1_missing_image_count")
    Long partOneMissingImageCount;

    @JsonProperty("listening_missing_audio_range_count")
    Long listeningMissingAudioRangeCount;

    @JsonProperty("reading_missing_passage_count")
    Long readingMissingPassageCount;

    @JsonProperty("publishable")
    boolean publishable;

    List<String> errors;
}
