package com.nhuhuy05.enghub.vocabulary.entity;

import com.nhuhuy05.enghub.user.entity.User;
import com.nhuhuy05.enghub.vocabulary.enums.VocabularyReviewRating;
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
@Table(name = "user_vocabulary_reviews")
public class UserVocabularyReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocabulary_id", nullable = false)
    Vocabulary vocabulary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    VocabularyReviewRating rating;

    @Column(name = "reviewed_at", nullable = false)
    LocalDateTime reviewedAt;
}
