package com.nhuhuy05.enghub.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnswerUpdateRequest {
    @JsonProperty("answer_text_en")
    String answerTextEn;

    @JsonProperty("answer_text_vi")
    String answerTextVi;

    @JsonProperty("is_correct")
    Boolean correct;
}
