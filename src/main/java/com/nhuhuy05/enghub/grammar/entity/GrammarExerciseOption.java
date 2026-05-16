package com.nhuhuy05.enghub.grammar.entity;

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
        name = "grammar_exercise_options",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_grammar_exercise_options_order", columnNames = {"exercise_id", "order_index"}),
                @UniqueConstraint(name = "uq_grammar_exercise_options_id_exercise_id", columnNames = {"id", "exercise_id"})
        }
)
public class GrammarExerciseOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    GrammarExercise exercise;

    @Column(name = "option_text", nullable = false, length = 1000)
    String optionText;

    @Column(name = "is_correct", nullable = false)
    boolean isCorrect;

    @Column(name = "order_index", nullable = false)
    Integer orderIndex;
}

