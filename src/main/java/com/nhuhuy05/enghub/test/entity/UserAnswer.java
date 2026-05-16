package com.nhuhuy05.enghub.test.entity;

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
        name = "user_answers",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_answers_attempt_question", columnNames = {"attempt_id", "question_id"})
)
public class UserAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    TestAttempt attempt;

    @Column(name = "question_id", nullable = false)
    Long questionId;

    @Column(name = "selected_answer_id")
    Long selectedAnswerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, insertable = false, updatable = false)
    Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "selected_answer_id", referencedColumnName = "id", insertable = false, updatable = false),
            @JoinColumn(name = "question_id", referencedColumnName = "question_id", insertable = false, updatable = false)
    })
    Answer selectedAnswer;

    @Column(name = "is_correct", nullable = false)
    boolean isCorrect;

    @Column(name = "answered_at", nullable = false)
    LocalDateTime answeredAt;
}
