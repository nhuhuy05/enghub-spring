package com.nhuhuy05.enghub.media.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaAssetResponse {
    Long id;

    @JsonProperty("test_id")
    Long testId;

    String label;

    @JsonProperty("media_type")
    String mediaType;

    @JsonProperty("cloudinary_public_id")
    String cloudinaryPublicId;

    String url;

    @JsonProperty("duration_ms")
    Integer durationMs;

    @JsonProperty("original_filename")
    String originalFilename;

    @JsonProperty("created_at")
    LocalDateTime createdAt;
}
