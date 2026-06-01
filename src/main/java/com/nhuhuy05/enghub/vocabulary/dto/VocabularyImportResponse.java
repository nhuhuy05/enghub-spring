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
public class VocabularyImportResponse {
    boolean success;

    @JsonProperty("topic_id")
    Long topicId;

    @JsonProperty("total_rows")
    Integer totalRows;

    @JsonProperty("created_count")
    Integer createdCount;

    @JsonProperty("updated_count")
    Integer updatedCount;

    @JsonProperty("skipped_count")
    Integer skippedCount;

    List<VocabularyImportErrorResponse> errors;
}
