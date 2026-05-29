package com.nhuhuy05.enghub.listening.entity;

import com.nhuhuy05.enghub.media.entity.MediaAsset;
import com.nhuhuy05.enghub.test.entity.QuestionGroup;
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
@Table(
        name = "question_group_audios",
        uniqueConstraints = @UniqueConstraint(name = "uq_question_group_audios_order", columnNames = {"question_group_id", "order_index"})
)
public class QuestionGroupAudio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_group_id", nullable = false)
    QuestionGroup questionGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_asset_id", nullable = false)
    MediaAsset mediaAsset;

    @Column(name = "start_ms", nullable = false)
    Integer startMs;

    @Column(name = "end_ms")
    Integer endMs;

    @Column(name = "transcript_en")
    String transcriptEn;

    @Column(name = "transcript_vi")
    String transcriptVi;

    @Column(name = "order_index", nullable = false)
    Integer orderIndex;
}
