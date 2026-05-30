package com.nhuhuy05.enghub.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiGenerationRequest {
    Boolean transcript;

    @JsonProperty("question_translation")
    Boolean questionTranslation;

    Boolean explanation;
    Boolean overwrite;

    public boolean transcriptEnabled() {
        return transcript == null || transcript;
    }

    public boolean questionTranslationEnabled() {
        return questionTranslation == null || questionTranslation;
    }

    public boolean explanationEnabled() {
        return explanation == null || explanation;
    }

    public boolean overwriteEnabled() {
        return Boolean.TRUE.equals(overwrite);
    }
}
