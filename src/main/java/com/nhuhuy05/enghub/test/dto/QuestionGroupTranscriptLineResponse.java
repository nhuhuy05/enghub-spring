package com.nhuhuy05.enghub.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionGroupTranscriptLineResponse {
    Long id;
    String speaker;

    @JsonProperty("text_en")
    String textEn;

    @JsonProperty("text_vi")
    String textVi;

    @JsonProperty("start_ms")
    Integer startMs;

    @JsonProperty("end_ms")
    Integer endMs;

    @JsonProperty("order_index")
    Integer orderIndex;
}
