package com.nhuhuy05.enghub.reading.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nhuhuy05.enghub.reading.enums.ReadingLessonStatus;
import com.nhuhuy05.enghub.reading.enums.ReadingLessonType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingLessonCreateRequest {
    @JsonProperty("question_group_id")
    @NotNull(message = "INVALID_KEY")
    Long questionGroupId;

    String title;

    @JsonProperty("title_vi")
    String titleVi;

    @JsonProperty("reading_type")
    ReadingLessonType readingType;

    ReadingLessonStatus status;

    String difficulty;
}
