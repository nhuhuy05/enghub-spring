package com.nhuhuy05.enghub.test.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttemptResultResponse {
    AttemptResponse attempt;
    List<Part> parts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Part {
        @JsonProperty("part_number")
        Integer partNumber;

        String title;
        List<Group> groups;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Group {
        Long id;

        @JsonProperty("group_order")
        Integer groupOrder;

        List<AttemptContentResponse.Image> images;
        AttemptContentResponse.Audio audio;
        List<AttemptContentResponse.Passage> passages;
        String transcriptEn;
        String transcriptVi;
        List<QuestionResult> questions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class QuestionResult {
        Long id;

        @JsonProperty("question_number")
        Integer questionNumber;

        @JsonProperty("question_text_en")
        String questionTextEn;

        @JsonProperty("question_text_vi")
        String questionTextVi;

        @JsonProperty("selected_answer_id")
        Long selectedAnswerId;

        @JsonProperty("correct_answer_id")
        Long correctAnswerId;

        Boolean correct;

        @JsonProperty("explanation_vi")
        String explanationVi;

        List<AnswerResult> answers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AnswerResult {
        Long id;
        String label;

        @JsonProperty("answer_text_en")
        String answerTextEn;

        @JsonProperty("answer_text_vi")
        String answerTextVi;

        @JsonProperty("is_correct")
        Boolean correct;
    }
}
