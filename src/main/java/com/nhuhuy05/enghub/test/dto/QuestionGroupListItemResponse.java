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
public class QuestionGroupListItemResponse {
    Long id;

    @JsonProperty("part_number")
    Integer partNumber;

    @JsonProperty("group_order")
    Integer groupOrder;

    @JsonProperty("question_numbers")
    List<Integer> questionNumbers;

    @JsonProperty("review_status")
    String reviewStatus;

    @JsonProperty("missing_flags")
    List<String> missingFlags;
}
