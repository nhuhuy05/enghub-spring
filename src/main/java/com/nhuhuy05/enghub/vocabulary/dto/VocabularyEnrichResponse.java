package com.nhuhuy05.enghub.vocabulary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VocabularyEnrichResponse {
    @JsonProperty("topic_id")
    Long topicId;

    @JsonProperty("vocabulary_id")
    Long vocabularyId;

    @JsonProperty("total_words")
    Integer totalWords;

    @JsonProperty("updated_count")
    Integer updatedCount;

    @JsonProperty("skipped_count")
    Integer skippedCount;

    List<VocabularyEnrichErrorResponse> errors;

    List<VocabularyResponse> words;
}
