package com.nhuhuy05.enghub.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportSummaryResponse {
    @JsonProperty("total_rows")
    Integer totalRows;

    @JsonProperty("valid_rows")
    Integer validRows;

    @JsonProperty("error_count")
    Integer errorCount;
}
