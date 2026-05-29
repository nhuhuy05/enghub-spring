package com.nhuhuy05.enghub.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionGroupTranscriptUpdateRequest {
    @JsonProperty("transcript_en")
    String transcriptEn;

    @JsonProperty("transcript_vi")
    String transcriptVi;
}
