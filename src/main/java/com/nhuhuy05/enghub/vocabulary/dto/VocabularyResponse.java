package com.nhuhuy05.enghub.vocabulary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VocabularyResponse {
    Long id;
    String word;

    @JsonProperty("meaning_vi")
    String meaningVi;

    @JsonProperty("meaning_en")
    String meaningEn;

    @JsonProperty("part_of_speech")
    String partOfSpeech;

    String pronunciation;

    @JsonProperty("example_sentence_en")
    String exampleSentenceEn;

    @JsonProperty("example_sentence_vi")
    String exampleSentenceVi;

    @JsonProperty("audio_url")
    String audioUrl;

    List<VocabularyTopicResponse> topics;
    VocabularyProgressResponse progress;

    @JsonProperty("review_options")
    List<VocabularyReviewOptionResponse> reviewOptions;

    @JsonProperty("created_at")
    LocalDateTime createdAt;

    @JsonProperty("updated_at")
    LocalDateTime updatedAt;
}
