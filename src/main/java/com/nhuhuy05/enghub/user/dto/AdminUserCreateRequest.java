package com.nhuhuy05.enghub.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUserCreateRequest {
    @Email(message = "INVALID_EMAIL")
    @NotBlank(message = "EMAIL_IS_REQUIRED")
    String email;

    @Size(min = 8, message = "INVALID_PASSWORD")
    @NotBlank(message = "INVALID_PASSWORD")
    String password;

    @JsonProperty("full_name")
    String fullName;

    String phone;

    @JsonProperty("avatar_url")
    String avatarUrl;

    Boolean active;

    List<String> roles;
}
