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
public class AttemptContentResponse {
    AttemptResponse attempt;

    @JsonProperty("test_id")
    Long testId;

    String title;
    String description;

    @JsonProperty("duration_minutes")
    Integer durationMinutes;

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

        List<Image> images;
        Audio audio;
        List<Passage> passages;
        List<QuestionItem> questions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Image {
        Long id;
        String label;
        String url;

        @JsonProperty("order_index")
        Integer orderIndex;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Audio {
        Long id;
        String label;
        String url;

        @JsonProperty("start_ms")
        Integer startMs;

        @JsonProperty("end_ms")
        Integer endMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Passage {
        Long id;
        String label;
        String url;
        String title;

        @JsonProperty("passage_type")
        String passageType;

        @JsonProperty("content_format")
        String contentFormat;

        @JsonProperty("content_en")
        String contentEn;

        @JsonProperty("content_vi")
        String contentVi;

        @JsonProperty("order_index")
        Integer orderIndex;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class QuestionItem {
        Long id;

        @JsonProperty("question_number")
        Integer questionNumber;

        @JsonProperty("question_text_en")
        String questionTextEn;

        @JsonProperty("question_text_vi")
        String questionTextVi;

        @JsonProperty("selected_answer_id")
        Long selectedAnswerId;

        List<AnswerItem> answers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AnswerItem {
        Long id;
        String label;

        @JsonProperty("answer_text_en")
        String answerTextEn;

        @JsonProperty("answer_text_vi")
        String answerTextVi;
    }
}
