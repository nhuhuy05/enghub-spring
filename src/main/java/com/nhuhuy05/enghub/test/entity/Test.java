package com.nhuhuy05.enghub.test.entity;

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
@Table(name = "tests")
public class Test {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 255)
    String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    TestCollection collection;

    @Column(name = "test_number")
    Integer testNumber;

    String description;

    @Column(name = "total_questions", nullable = false)
    Integer totalQuestions;

    @Column(name = "duration_minutes", nullable = false)
    Integer durationMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    Boolean published = false;

    @Column(name = "workflow_status", nullable = false, length = 50)
    @Builder.Default
    String workflowStatus = "draft";

    @OneToMany(mappedBy = "test", fetch = FetchType.LAZY)
    Set<TestPart> parts;

    @PrePersist
    void prePersist() {
        if (published == null) {
            published = false;
        }
        if (workflowStatus == null) {
            workflowStatus = "draft";
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
