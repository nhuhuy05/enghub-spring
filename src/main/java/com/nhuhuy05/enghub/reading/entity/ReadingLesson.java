package com.nhuhuy05.enghub.reading.entity;

import com.nhuhuy05.enghub.reading.enums.ReadingLessonStatus;
import com.nhuhuy05.enghub.reading.enums.ReadingLessonType;
import com.nhuhuy05.enghub.test.entity.QuestionGroup;
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
        name = "reading_lessons",
        uniqueConstraints = @UniqueConstraint(name = "uq_reading_lessons_question_group", columnNames = "question_group_id")
)
public class ReadingLesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_group_id", nullable = false)
    QuestionGroup questionGroup;

    @Column(nullable = false, length = 255)
    String title;

    @Column(name = "title_vi", length = 255)
    String titleVi;

    @Enumerated(EnumType.STRING)
    @Column(name = "reading_type", nullable = false, length = 20)
    ReadingLessonType readingType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    ReadingLessonStatus status = ReadingLessonStatus.DRAFT;

    @Column(length = 50)
    String difficulty;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @OneToMany(mappedBy = "readingLesson", fetch = FetchType.LAZY)
    Set<ReadingVocabularyHint> vocabularyHints;

    @PrePersist
    void prePersist() {
        if (status == null) {
            status = ReadingLessonStatus.DRAFT;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
