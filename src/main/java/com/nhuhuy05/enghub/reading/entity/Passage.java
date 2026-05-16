package com.nhuhuy05.enghub.reading.entity;

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
        name = "passages",
        uniqueConstraints = @UniqueConstraint(name = "uq_passages_group_order", columnNames = {"question_group_id", "order_index"})
)
public class Passage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_group_id", nullable = false)
    QuestionGroup questionGroup;

    @Column(length = 255)
    String title;

    @Column(name = "passage_type", length = 100)
    String passageType;

    @Column(name = "content_format", length = 50)
    String contentFormat;

    @Column(name = "content_en")
    String contentEn;

    @Column(name = "content_vi")
    String contentVi;

    @Column(name = "vocab_hints")
    String vocabHints;

    @Column(name = "image_url", length = 500)
    String imageUrl;

    @Column(name = "order_index", nullable = false)
    Integer orderIndex;
}

