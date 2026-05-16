package com.nhuhuy05.enghub.vocabulary.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

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

    @Column(length = 255)
    String pronunciation;

    @Column(name = "example_sentence")
    String exampleSentence;

    @Column(name = "audio_url", length = 500)
    String audioUrl;
}

