package com.nhuhuy05.enghub.reading.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nhuhuy05.enghub.reading.enums.ReadingLessonStatus;
import com.nhuhuy05.enghub.reading.enums.ReadingLessonType;
import jakarta.validation.Valid;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingLessonUpdateRequest {
    String title;

    @JsonProperty("title_vi")
    String titleVi;

    @JsonProperty("reading_type")
    ReadingLessonType readingType;

    ReadingLessonStatus status;

    String difficulty;

    @Valid
    List<PassageItem> passages;

    @JsonProperty("vocabulary_hints")
    @Valid
    List<VocabularyHintItem> vocabularyHints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PassageItem {
        @JsonProperty("media_asset_id")
        Long mediaAssetId;

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
    public static class VocabularyHintItem {
        @JsonProperty("passage_id")
        Long passageId;

        @JsonProperty("passage_order_index")
        Integer passageOrderIndex;

        String word;

        @JsonProperty("part_of_speech")
        String partOfSpeech;

        @JsonProperty("meaning_vi")
        String meaningVi;

        @JsonProperty("order_index")
        Integer orderIndex;
    }
}
