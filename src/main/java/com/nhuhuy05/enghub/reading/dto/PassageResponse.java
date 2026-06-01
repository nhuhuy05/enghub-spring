package com.nhuhuy05.enghub.reading.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PassageResponse {
    Long id;

    @JsonProperty("question_group_id")
    Long questionGroupId;

    @JsonProperty("part_number")
    Integer partNumber;

    @JsonProperty("group_order")
    Integer groupOrder;

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

    @JsonProperty("media_asset_id")
    Long mediaAssetId;

    @JsonProperty("media_label")
    String mediaLabel;

    @JsonProperty("media_url")
    String mediaUrl;

    @JsonProperty("order_index")
    Integer orderIndex;
}
