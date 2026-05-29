package com.nhuhuy05.enghub.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionGroupPassagesUpdateRequest {
    @Valid
    @NotNull(message = "INVALID_KEY")
    List<Item> passages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Item {
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

        @JsonProperty("vocab_hints")
        String vocabHints;

        @JsonProperty("order_index")
        Integer orderIndex;
    }
}
