package com.nhuhuy05.enghub.test.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportErrorResponse {
    Integer row;
    String field;
    String message;
}
