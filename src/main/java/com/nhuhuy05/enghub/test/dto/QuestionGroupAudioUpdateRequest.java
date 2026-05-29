package com.nhuhuy05.enghub.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionGroupAudioUpdateRequest {
    @JsonProperty("media_asset_id")
    Long mediaAssetId;

    @JsonProperty("start_ms")
    @Min(value = 0, message = "INVALID_KEY")
    Integer startMs;

    @JsonProperty("end_ms")
    Integer endMs;

    @JsonProperty("transcript_en")
    String transcriptEn;

    @JsonProperty("transcript_vi")
    String transcriptVi;
}
