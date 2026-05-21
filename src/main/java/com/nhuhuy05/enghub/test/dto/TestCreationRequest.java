package com.nhuhuy05.enghub.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestCreationRequest {
    @JsonProperty("collection_id")
    Long collectionId;

    @JsonProperty("test_number")
    @Min(value = 1, message = "INVALID_KEY")
    Integer testNumber;

    @NotBlank(message = "INVALID_KEY")
    String title;

    String description;

    @JsonProperty("duration_minutes")
    @NotNull(message = "INVALID_KEY")
    @Min(value = 0, message = "INVALID_KEY")
    Integer durationMinutes;

    @JsonProperty("total_questions")
    @Min(value = 0, message = "INVALID_KEY")
    Integer totalQuestions;
}
