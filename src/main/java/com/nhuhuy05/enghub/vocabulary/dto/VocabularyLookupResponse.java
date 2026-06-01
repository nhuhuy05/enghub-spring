package com.nhuhuy05.enghub.vocabulary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VocabularyLookupResponse {
    String word;

    @JsonProperty("meaning_en")
    String meaningEn;

    @JsonProperty("meaning_vi")
    String meaningVi;

    @JsonProperty("part_of_speech")
    String partOfSpeech;

    String pronunciation;

    @JsonProperty("example_sentence_en")
    String exampleSentenceEn;

    @JsonProperty("example_sentence_vi")
    String exampleSentenceVi;

    @JsonProperty("audio_url")
    String audioUrl;
}
