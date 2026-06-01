package com.nhuhuy05.enghub.listening.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListeningDictationSessionResponse {
    @JsonProperty("test_id")
    Long testId;

    @JsonProperty("part_id")
    String partId;

    @JsonProperty("part_number")
    Integer partNumber;

    String title;

    @JsonProperty("part_name")
    String partName;

    String instruction;
    List<Group> groups;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Group {
        Long id;
        String title;

        @JsonProperty("group_order")
        Integer groupOrder;

        @JsonProperty("question_numbers")
        List<Integer> questionNumbers;

        List<Sentence> sentences;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Sentence {
        String id;
        String speaker;
        String text;
        String translation;

        @JsonProperty("audio_url")
        String audioUrl;

        @JsonProperty("start_ms")
        Integer startMs;

        @JsonProperty("end_ms")
        Integer endMs;

        @JsonProperty("order_index")
        Integer orderIndex;

        Boolean completed;

        @JsonProperty("hint_levels")
        List<Integer> hintLevels;
    }
}
