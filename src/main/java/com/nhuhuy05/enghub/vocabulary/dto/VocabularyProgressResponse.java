package com.nhuhuy05.enghub.vocabulary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VocabularyProgressResponse {
    Long id;

    @JsonProperty("vocabulary_id")
    Long vocabularyId;

    Integer level;

    @JsonProperty("learned_at")
    LocalDateTime learnedAt;

    @JsonProperty("last_reviewed_at")
    LocalDateTime lastReviewedAt;

    @JsonProperty("next_review_at")
    LocalDateTime nextReviewAt;

    @JsonProperty("review_count")
    Integer reviewCount;

    @JsonProperty("correct_count")
    Integer correctCount;

    @JsonProperty("interval_days")
    Integer intervalDays;

    @JsonProperty("ease_factor")
    BigDecimal easeFactor;

    boolean mastered;
}
