package com.nhuhuy05.enghub.test.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SaveAnswerRequest {
    Long questionId;
    Long selectedAnswerId;
}
