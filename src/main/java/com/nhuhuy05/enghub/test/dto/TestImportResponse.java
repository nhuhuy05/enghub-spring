package com.nhuhuy05.enghub.test.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestImportResponse {
    boolean success;
    ImportSummaryResponse summary;
    List<ImportErrorResponse> errors;
}
