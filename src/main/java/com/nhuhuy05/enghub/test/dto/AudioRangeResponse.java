package com.nhuhuy05.enghub.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AudioRangeResponse {
    Long id;

    @JsonProperty("question_group_id")
    Long questionGroupId;

    @JsonProperty("part_number")
    Integer partNumber;

    @JsonProperty("group_order")
    Integer groupOrder;

    @JsonProperty("media_asset_id")
    Long mediaAssetId;

    @JsonProperty("start_ms")
    Integer startMs;

    @JsonProperty("end_ms")
    Integer endMs;
}
