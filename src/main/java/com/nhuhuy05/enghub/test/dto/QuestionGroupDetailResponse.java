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
public class QuestionGroupDetailResponse {
    Long id;

    @JsonProperty("part_number")
    Integer partNumber;

    @JsonProperty("group_order")
    Integer groupOrder;

    @JsonProperty("review_status")
    String reviewStatus;

    List<GroupImageResponse> images;
    GroupAudioResponse audio;
    List<GroupPassageResponse> passages;
    List<GroupQuestionResponse> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GroupImageResponse {
        Long id;

        @JsonProperty("media_asset_id")
        Long mediaAssetId;

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
    public static class GroupAudioResponse {
        Long id;

        @JsonProperty("media_asset_id")
        Long mediaAssetId;

        String label;
        String url;

        @JsonProperty("start_ms")
        Integer startMs;

        @JsonProperty("end_ms")
        Integer endMs;

        @JsonProperty("transcript_en")
        String transcriptEn;

        @JsonProperty("transcript_vi")
        String transcriptVi;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GroupPassageResponse {
        Long id;

        @JsonProperty("media_asset_id")
        Long mediaAssetId;

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

        @JsonProperty("vocab_hints")
        String vocabHints;

        @JsonProperty("order_index")
        Integer orderIndex;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GroupQuestionResponse {
        Long id;

        @JsonProperty("question_number")
        Integer questionNumber;

        @JsonProperty("question_text_en")
        String questionTextEn;

        @JsonProperty("question_text_vi")
        String questionTextVi;

        @JsonProperty("explanation_vi")
        String explanationVi;

        List<GroupAnswerResponse> answers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GroupAnswerResponse {
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
