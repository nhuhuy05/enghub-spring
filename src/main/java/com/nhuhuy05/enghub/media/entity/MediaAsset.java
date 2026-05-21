package com.nhuhuy05.enghub.media.entity;

import com.nhuhuy05.enghub.test.entity.Test;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "media_assets",
        uniqueConstraints = @UniqueConstraint(name = "uq_ma_label", columnNames = {"test_id", "label", "media_type"})
)
public class MediaAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false)
    Test test;

    @Column(nullable = false, length = 100)
    String label;

    @Column(name = "media_type", nullable = false, length = 20)
    String mediaType;

    @Column(name = "cloudinary_public_id", nullable = false, length = 255)
    String cloudinaryPublicId;

    @Column(nullable = false, length = 500)
    String url;

    @Column(name = "duration_ms")
    Integer durationMs;

    @Column(name = "original_filename", length = 255)
    String originalFilename;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
