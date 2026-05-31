package com.nhuhuy05.enghub.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nhuhuy05.enghub.common.enums.AttemptMode;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StartAttemptRequest {
    Long testId;
    AttemptMode mode;

    @JsonProperty("part_numbers")
    List<Integer> partNumbers;
}
