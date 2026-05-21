package com.nhuhuy05.enghub.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AudioRangeUpdateRequest {
    @JsonProperty("part_number")
    @NotNull(message = "INVALID_KEY")
    Integer partNumber;

    @JsonProperty("group_order")
    @NotNull(message = "INVALID_KEY")
    Integer groupOrder;

    @JsonProperty("start_ms")
    @NotNull(message = "INVALID_KEY")
    @Min(value = 0, message = "INVALID_KEY")
    Integer startMs;

    @JsonProperty("end_ms")
    Integer endMs;
}
