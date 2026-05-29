package com.nhuhuy05.enghub.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionUpdateRequest {
    @JsonProperty("question_text_en")
    String questionTextEn;

    @JsonProperty("question_text_vi")
    String questionTextVi;

    @JsonProperty("explanation_vi")
    String explanationVi;
}
