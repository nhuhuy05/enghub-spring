package com.nhuhuy05.enghub.vocabulary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VocabularyEnrichRequest {
    @JsonProperty("lookup_en")
    Boolean lookupEn;

    @JsonProperty("translate_vi")
    Boolean translateVi;

    Boolean overwrite;
}
