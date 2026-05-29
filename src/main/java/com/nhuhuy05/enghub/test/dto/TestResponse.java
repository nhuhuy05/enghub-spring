package com.nhuhuy05.enghub.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestResponse {
    Long id;

    @JsonProperty("collection_id")
    Long collectionId;

    @JsonProperty("collection_name")
    String collectionName;

    @JsonProperty("test_number")
    Integer testNumber;

    String title;
    String description;

    @JsonProperty("total_questions")
    Integer totalQuestions;

    @JsonProperty("duration_minutes")
    Integer durationMinutes;

    @JsonProperty("is_published")
    Boolean published;

    @JsonProperty("workflow_status")
    String workflowStatus;

    @JsonProperty("created_at")
    LocalDateTime createdAt;
}
