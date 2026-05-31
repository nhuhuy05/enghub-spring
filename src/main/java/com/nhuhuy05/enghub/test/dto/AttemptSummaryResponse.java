package com.nhuhuy05.enghub.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nhuhuy05.enghub.common.enums.AttemptMode;
import com.nhuhuy05.enghub.common.enums.AttemptStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttemptSummaryResponse {
    Long id;

    @JsonProperty("test_id")
    Long testId;

    @JsonProperty("test_title")
    String testTitle;

    AttemptMode mode;
    AttemptStatus status;

    @JsonProperty("correct_count")
    Integer correctCount;

    @JsonProperty("listening_correct")
    Integer listeningCorrect;

    @JsonProperty("reading_correct")
    Integer readingCorrect;

    @JsonProperty("answered_count")
    Integer answeredCount;

    @JsonProperty("total_questions")
    Integer totalQuestions;

    @JsonProperty("total_score")
    Integer totalScore;

    @JsonProperty("reading_score")
    Integer readingScore;

    @JsonProperty("listening_score")
    Integer listeningScore;

    @JsonProperty("duration_seconds")
    Integer durationSeconds;

    @JsonProperty("started_at")
    LocalDateTime startedAt;

    @JsonProperty("submitted_at")
    LocalDateTime submittedAt;

    @JsonProperty("expires_at")
    LocalDateTime expiresAt;

    @JsonProperty("remaining_seconds")
    Long remainingSeconds;

    @JsonProperty("part_numbers")
    List<Integer> partNumbers;
}
