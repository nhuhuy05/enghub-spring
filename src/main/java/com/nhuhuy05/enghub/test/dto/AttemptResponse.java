package com.nhuhuy05.enghub.test.dto;

import com.nhuhuy05.enghub.common.enums.AttemptMode;
import com.nhuhuy05.enghub.common.enums.AttemptStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttemptResponse {
    Long id;
    Long testId;
    AttemptMode mode;
    AttemptStatus status;
    Integer correctCount;
    Integer totalQuestions;
    Integer totalScore;
    Integer readingScore;
    Integer listeningScore;
    LocalDateTime startedAt;
    LocalDateTime submittedAt;
}
