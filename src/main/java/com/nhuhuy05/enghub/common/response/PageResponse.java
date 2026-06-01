package com.nhuhuy05.enghub.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponse<T> {
    List<T> content;
    int page;
    int size;

    @JsonProperty("total_elements")
    long totalElements;

    @JsonProperty("total_pages")
    int totalPages;

    boolean first;
    boolean last;
}
