package com.nhuhuy05.enghub.vocabulary.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VocabularyImportErrorResponse {
    Integer row;
    String field;
    String message;
}
