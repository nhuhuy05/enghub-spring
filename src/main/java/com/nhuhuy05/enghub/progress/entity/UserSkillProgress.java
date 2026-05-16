package com.nhuhuy05.enghub.progress.entity;

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
@Table(
        name = "user_skill_progress",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_skill_progress_user", columnNames = "user_id")
)
public class UserSkillProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "vocab_learned", nullable = false)
    Integer vocabLearned;

    @Column(name = "vocab_due_today", nullable = false)
    Integer vocabDueToday;

    @Column(name = "grammar_completed", nullable = false)
    Integer grammarCompleted;

    @Column(name = "listening_completed", nullable = false)
    Integer listeningCompleted;

    @Column(name = "reading_completed", nullable = false)
    Integer readingCompleted;

    @Column(name = "total_study_minutes", nullable = false)
    Integer totalStudyMinutes;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}

