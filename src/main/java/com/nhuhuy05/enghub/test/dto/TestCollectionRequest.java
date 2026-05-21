package com.nhuhuy05.enghub.test.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestCollectionRequest {
    @NotBlank(message = "INVALID_KEY")
    String name;

    String description;
}
