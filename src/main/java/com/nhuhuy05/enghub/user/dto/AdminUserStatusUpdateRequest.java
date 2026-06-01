package com.nhuhuy05.enghub.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUserStatusUpdateRequest {
    @NotNull(message = "INVALID_KEY")
    Boolean active;
}
