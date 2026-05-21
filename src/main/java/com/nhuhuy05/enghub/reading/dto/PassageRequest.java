package com.nhuhuy05.enghub.reading.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PassageRequest {
    @JsonProperty("test_id")
    @NotNull(message = "INVALID_KEY")
    Long testId;

    @JsonProperty("part_number")
    @NotNull(message = "INVALID_KEY")
    Integer partNumber;

    @JsonProperty("group_order")
    @NotNull(message = "INVALID_KEY")
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

    @JsonProperty("order_index")
    Integer orderIndex;
}
