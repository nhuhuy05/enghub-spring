package com.nhuhuy05.enghub.vocabulary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VocabularyRequest {
    @NotBlank(message = "INVALID_KEY")
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

    @JsonProperty("topic_ids")
    List<Long> topicIds;
}
