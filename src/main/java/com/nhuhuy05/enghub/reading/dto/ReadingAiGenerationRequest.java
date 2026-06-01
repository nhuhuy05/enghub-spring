package com.nhuhuy05.enghub.reading.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingAiGenerationRequest {
    @JsonProperty("overwrite_enabled")
    Boolean overwriteEnabled;

    public boolean overwriteEnabled() {
        return Boolean.TRUE.equals(overwriteEnabled);
    }
}
