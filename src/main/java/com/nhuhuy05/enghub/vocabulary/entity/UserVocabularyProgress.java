package com.nhuhuy05.enghub.vocabulary.entity;

import com.nhuhuy05.enghub.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "user_vocabulary_progress",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_vocabulary_progress", columnNames = {"user_id", "vocabulary_id"})
)
public class UserVocabularyProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocabulary_id", nullable = false)
    Vocabulary vocabulary;

    @Column(nullable = false)
    Integer level;

    @Column(name = "next_review_at")
    LocalDateTime nextReviewAt;

    @Column(name = "last_reviewed_at")
    LocalDateTime lastReviewedAt;

    @Column(name = "review_count", nullable = false)
    Integer reviewCount;

    @Column(name = "interval_days", nullable = false)
    Integer intervalDays;

    @Column(name = "ease_factor", nullable = false, precision = 4, scale = 2)
    BigDecimal easeFactor;
}
