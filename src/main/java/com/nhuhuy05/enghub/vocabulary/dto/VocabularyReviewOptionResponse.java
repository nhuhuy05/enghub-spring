package com.nhuhuy05.enghub.vocabulary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nhuhuy05.enghub.vocabulary.enums.VocabularyReviewRating;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VocabularyReviewOptionResponse {
    VocabularyReviewRating rating;
    String label;

    @JsonProperty("delay_label")
    String delayLabel;

    @JsonProperty("next_review_at")
    LocalDateTime nextReviewAt;
}
