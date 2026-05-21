package com.nhuhuy05.enghub.user.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uq_users_provider_provider_id", columnNames = {"provider", "provider_id"})
        }
)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 255)
    String email;

    @Column(length = 255)
    String password;

    @Column(name = "full_name", length = 255)
    String fullName;

    @Column(length = 30)
    String phone;

    @Column(name = "avatar_url", length = 500)
    String avatarUrl;

    @Column(length = 50)
    String provider;

    @Column(name = "provider_id", length = 255)
    String providerId;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    LocalDateTime createdAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    Boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    Set<Role> roles;

    @PrePersist
    void prePersist() {
        if (active == null) {
            active = true;
        }
    }
}



