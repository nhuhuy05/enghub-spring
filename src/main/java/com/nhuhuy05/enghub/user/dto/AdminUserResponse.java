package com.nhuhuy05.enghub.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUserResponse {
    Long id;
    String email;

    @JsonProperty("full_name")
    String fullName;

    String phone;

    @JsonProperty("avatar_url")
    String avatarUrl;

    String provider;
    Boolean active;
    Set<RoleResponse> roles;

    @JsonProperty("created_at")
    LocalDateTime createdAt;
}
