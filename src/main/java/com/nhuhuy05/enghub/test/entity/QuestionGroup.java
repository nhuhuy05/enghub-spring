package com.nhuhuy05.enghub.test.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "question_groups",
        uniqueConstraints = @UniqueConstraint(name = "uq_question_groups_order", columnNames = {"test_part_id", "order_index"})
)
public class QuestionGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_part_id", nullable = false)
    TestPart testPart;

    @Column(length = 255)
    String title;

    @Column(name = "question_count", nullable = false)
    Integer questionCount;

    @Column(name = "order_index", nullable = false)
    Integer orderIndex;

    @OneToMany(mappedBy = "questionGroup", fetch = FetchType.LAZY)
    Set<Question> questions;
}
