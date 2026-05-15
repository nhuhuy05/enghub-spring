package com.nhuhuy05.enghub.test.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAnswerResponse {
    Long attemptId;
    Long questionId;
    Long selectedAnswerId;
    boolean correct;
    LocalDateTime answeredAt;
}
