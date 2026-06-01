package com.nhuhuy05.enghub.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionGroupTranscriptLinesUpdateRequest {
    @NotNull(message = "INVALID_KEY")
    @Valid
    List<Line> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Line {
        Long id;

        @Size(max = 100, message = "INVALID_KEY")
        String speaker;

        @JsonProperty("text_en")
        @NotBlank(message = "INVALID_KEY")
        String textEn;

        @JsonProperty("text_vi")
        String textVi;

        @JsonProperty("start_ms")
        @Min(value = 0, message = "INVALID_KEY")
        Integer startMs;

        @JsonProperty("end_ms")
        @Min(value = 0, message = "INVALID_KEY")
        Integer endMs;

        @JsonProperty("order_index")
        @NotNull(message = "INVALID_KEY")
        @Min(value = 0, message = "INVALID_KEY")
        Integer orderIndex;
    }
}
