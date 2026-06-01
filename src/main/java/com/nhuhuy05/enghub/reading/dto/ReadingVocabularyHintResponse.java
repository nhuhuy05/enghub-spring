package com.nhuhuy05.enghub.reading.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingVocabularyHintResponse {
    Long id;

    @JsonProperty("passage_id")
    Long passageId;

    @JsonProperty("passage_order_index")
    Integer passageOrderIndex;

    String word;

    @JsonProperty("part_of_speech")
    String partOfSpeech;

    @JsonProperty("meaning_vi")
    String meaningVi;

    @JsonProperty("order_index")
    Integer orderIndex;
}
