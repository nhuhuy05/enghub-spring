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
@Table(name = "vocabulary")
public class Vocabulary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true, length = 255)
    String word;

    @Column(name = "meaning_vi", length = 1000)
    String meaningVi;

    @Column(name = "meaning_en", length = 1000)
    String meaningEn;

    @Column(name = "part_of_speech", length = 50)
    String partOfSpeech;

    @Column(length = 255)
    String pronunciation;

    @Column(name = "example_sentence")
    String exampleSentence;

    @Column(name = "example_sentence_vi")
    String exampleSentenceVi;

    @Column(name = "audio_url", length = 500)
    String audioUrl;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    LocalDateTime updatedAt;
}
