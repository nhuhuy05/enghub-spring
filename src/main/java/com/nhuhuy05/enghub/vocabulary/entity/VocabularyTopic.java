package com.nhuhuy05.enghub.vocabulary.entity;

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
@Table(name = "vocabulary_topics")
public class VocabularyTopic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true, length = 255)
    String name;

    String description;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    LocalDateTime updatedAt;
}
