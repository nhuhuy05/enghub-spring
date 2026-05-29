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
public class QuestionGroupImagesUpdateRequest {
    @Valid
    @NotNull(message = "INVALID_KEY")
    List<Item> images;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Item {
        @JsonProperty("media_asset_id")
        @NotNull(message = "INVALID_KEY")
        Long mediaAssetId;

        @JsonProperty("order_index")
        Integer orderIndex;
    }
}
