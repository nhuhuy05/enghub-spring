package com.nhuhuy05.enghub.reading.entity;

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
@Table(name = "reading_vocabulary_hints")
public class ReadingVocabularyHint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reading_lesson_id", nullable = false)
    ReadingLesson readingLesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passage_id")
    QuestionGroupPassage passage;

    @Column(nullable = false, length = 150)
    String word;

    @Column(name = "part_of_speech", length = 50)
    String partOfSpeech;

    @Column(name = "meaning_vi", nullable = false)
    String meaningVi;

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
