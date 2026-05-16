package com.nhuhuy05.enghub.listening.entity;

import com.nhuhuy05.enghub.test.entity.TestPart;
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
        name = "part_audio_ranges",
        uniqueConstraints = @UniqueConstraint(name = "uq_part_audio_ranges_order", columnNames = {"test_part_id", "order_index"})
)
public class PartAudioRange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_part_id", nullable = false)
    TestPart testPart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audio_file_id", nullable = false)
    AudioFile audioFile;

    @Column(name = "start_ms", nullable = false)
    Integer startMs;

    @Column(name = "end_ms")
    Integer endMs;

    @Column(name = "order_index", nullable = false)
    Integer orderIndex;
}
