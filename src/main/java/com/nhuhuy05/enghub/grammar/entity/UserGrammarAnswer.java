package com.nhuhuy05.enghub.grammar.entity;

import com.nhuhuy05.enghub.user.entity.User;
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
@Table(name = "user_grammar_answers")
public class UserGrammarAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "exercise_id", nullable = false)
    Long exerciseId;

    @Column(name = "selected_option_id")
    Long selectedOptionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false, insertable = false, updatable = false)
    GrammarExercise exercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "selected_option_id", referencedColumnName = "id", insertable = false, updatable = false),
            @JoinColumn(name = "exercise_id", referencedColumnName = "exercise_id", insertable = false, updatable = false)
    })
    GrammarExerciseOption selectedOption;

    @Column(name = "is_correct", nullable = false)
    boolean isCorrect;

    @Column(name = "answered_at", nullable = false)
    LocalDateTime answeredAt;
}
