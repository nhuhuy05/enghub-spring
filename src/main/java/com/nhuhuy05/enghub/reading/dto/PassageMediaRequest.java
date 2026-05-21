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
public class PassageMediaRequest {
    @JsonProperty("media_asset_id")
    @NotNull(message = "INVALID_KEY")
    Long mediaAssetId;
}
