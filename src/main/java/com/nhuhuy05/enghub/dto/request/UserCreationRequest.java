package com.nhuhuy05.enghub.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {
    @jakarta.validation.constraints.Email(message = "INVALID_EMAIL")
    @jakarta.validation.constraints.NotBlank(message = "EMAIL_IS_REQUIRED")
    String email;

    @Size(min = 8, message = "INVALID_PASSWORD")
    String password;

    String fullName;
    String phone;
    String avatarUrl;
}
