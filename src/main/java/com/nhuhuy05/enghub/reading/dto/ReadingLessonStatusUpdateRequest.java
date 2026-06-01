package com.nhuhuy05.enghub.reading.dto;

import com.nhuhuy05.enghub.reading.enums.ReadingLessonStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadingLessonStatusUpdateRequest {
    @NotNull(message = "INVALID_KEY")
    ReadingLessonStatus status;
}
