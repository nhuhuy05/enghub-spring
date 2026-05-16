package com.nhuhuy05.enghub.listening.entity;

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
@Table(name = "audio_files")
public class AudioFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false)
    Test test;

    @Column(name = "audio_url", nullable = false, length = 500)
    String audioUrl;

    @Column(name = "duration_ms")
    Integer durationMs;

    @Column(length = 255)
    String title;

    @Column(name = "audio_type", length = 100)
    String audioType;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}

