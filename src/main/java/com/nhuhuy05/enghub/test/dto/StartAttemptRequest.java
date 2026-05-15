package com.nhuhuy05.enghub.test.dto;

import com.nhuhuy05.enghub.common.enums.AttemptMode;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StartAttemptRequest {
    Long testId;
    AttemptMode mode;
}
