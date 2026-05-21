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
        name = "questions",
        uniqueConstraints = @UniqueConstraint(name = "uq_questions_group_number", columnNames = {"question_group_id", "question_number"})
)
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_group_id", nullable = false)
    QuestionGroup questionGroup;

    @Column(name = "question_number", nullable = false)
    Integer questionNumber;

    @Column(name = "question_text")
    String questionText;

    String explanation;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    Set<Answer> answers;
}
