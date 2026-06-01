package com.nhuhuy05.enghub.reading.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nhuhuy05.enghub.reading.enums.ReadingLessonType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingPart7CandidateResponse {
    @JsonProperty("question_group_id")
    Long questionGroupId;

    @JsonProperty("test_id")
    Long testId;

    @JsonProperty("test_title")
    String testTitle;

    @JsonProperty("group_order")
    Integer groupOrder;

    @JsonProperty("question_numbers")
    List<Integer> questionNumbers;

    @JsonProperty("passage_count")
    Integer passageCount;

    @JsonProperty("suggested_reading_type")
    ReadingLessonType suggestedReadingType;

    @JsonProperty("existing_lesson_id")
    Long existingLessonId;

    String title;
}
