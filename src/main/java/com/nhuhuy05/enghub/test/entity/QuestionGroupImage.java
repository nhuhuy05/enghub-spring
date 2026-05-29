package com.nhuhuy05.enghub.test.entity;

import com.nhuhuy05.enghub.media.entity.MediaAsset;
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
        name = "question_group_images",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_question_group_images_order", columnNames = {"question_group_id", "order_index"}),
                @UniqueConstraint(name = "uq_question_group_images_media", columnNames = {"question_group_id", "media_asset_id"})
        }
)
public class QuestionGroupImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_group_id", nullable = false)
    QuestionGroup questionGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_asset_id", nullable = false)
    MediaAsset mediaAsset;

    @Column(name = "order_index", nullable = false)
    Integer orderIndex;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
