package com.nhuhuy05.enghub.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestPreviewContentResponse {
    @JsonProperty("test_id")
    Long testId;

    String title;
    String description;

    @JsonProperty("duration_minutes")
    Integer durationMinutes;

    List<PartResponse> parts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PartResponse {
        @JsonProperty("part_number")
        Integer partNumber;

        String title;
        List<QuestionGroupDetailResponse> groups;
    }
}
