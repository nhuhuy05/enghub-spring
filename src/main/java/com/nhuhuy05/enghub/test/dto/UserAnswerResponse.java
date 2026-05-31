package com.nhuhuy05.enghub.test.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAnswerResponse {
    Long attemptId;
    Long questionId;
    Long selectedAnswerId;
    Boolean correct;

    @JsonProperty("correct_answer_id")
    Long correctAnswerId;

    @JsonProperty("explanation_vi")
    String explanationVi;

    @JsonProperty("transcript_en")
    String transcriptEn;

    @JsonProperty("transcript_vi")
    String transcriptVi;

    LocalDateTime answeredAt;
}
