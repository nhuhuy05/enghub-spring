package com.nhuhuy05.enghub.test.entity;

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
        name = "answers",
        uniqueConstraints = @UniqueConstraint(name = "uq_answers_id_question_id", columnNames = {"id", "question_id"})
)
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    Question question;

    @Column(name = "answer_text_en", nullable = false)
    String answerTextEn;

    @Column(name = "answer_text_vi")
    String answerTextVi;

    @Column(name = "is_correct", nullable = false)
    boolean isCorrect;
}
