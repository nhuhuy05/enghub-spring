package com.nhuhuy05.enghub.listening.entity;

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
        name = "question_group_transcript_lines",
        uniqueConstraints = @UniqueConstraint(name = "uq_qgtl_audio_order", columnNames = {"question_group_audio_id", "order_index"})
)
public class QuestionGroupTranscriptLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_group_audio_id", nullable = false)
    QuestionGroupAudio questionGroupAudio;

    @Column(length = 100)
    String speaker;

    @Column(name = "text_en", nullable = false)
    String textEn;

    @Column(name = "text_vi")
    String textVi;

    @Column(name = "start_ms")
    Integer startMs;

    @Column(name = "end_ms")
    Integer endMs;

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
