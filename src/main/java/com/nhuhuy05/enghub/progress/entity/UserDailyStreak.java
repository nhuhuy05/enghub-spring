package com.nhuhuy05.enghub.progress.entity;

import com.nhuhuy05.enghub.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "user_daily_streak",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_daily_streak_user_date", columnNames = {"user_id", "study_date"})
)
public class UserDailyStreak {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "study_date", nullable = false)
    LocalDate studyDate;

    @Column(name = "minutes_studied", nullable = false)
    Integer minutesStudied;

    @Column(name = "vocab_done", nullable = false)
    boolean vocabDone;

    @Column(name = "grammar_done", nullable = false)
    boolean grammarDone;

    @Column(name = "listening_done", nullable = false)
    boolean listeningDone;
}

