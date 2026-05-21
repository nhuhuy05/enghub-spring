package com.nhuhuy05.enghub.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestPartResponse {
    Long id;

    @JsonProperty("test_id")
    Long testId;

    @JsonProperty("part_number")
    Integer partNumber;

    String title;
}
