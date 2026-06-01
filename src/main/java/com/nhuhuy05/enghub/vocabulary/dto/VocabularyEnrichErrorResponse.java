package com.nhuhuy05.enghub.vocabulary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VocabularyEnrichErrorResponse {
    @JsonProperty("vocabulary_id")
    Long vocabularyId;

    String word;
    String message;
}
