package com.nhuhuy05.enghub.vocabulary.dto;

import com.nhuhuy05.enghub.vocabulary.enums.VocabularyReviewRating;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VocabularyReviewRequest {
    @NotNull(message = "INVALID_KEY")
    VocabularyReviewRating rating;
}
