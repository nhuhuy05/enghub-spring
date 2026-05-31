package com.nhuhuy05.enghub.test.entity;

import com.nhuhuy05.enghub.common.enums.AttemptMode;
import com.nhuhuy05.enghub.common.enums.AttemptStatus;
import com.nhuhuy05.enghub.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "test_attempts")
public class TestAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false)
    Test test;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    AttemptMode mode;

    @Column(name = "started_at", nullable = false, updatable = false)
    LocalDateTime startedAt;

    @Column(name = "submitted_at")
    LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    AttemptStatus status;

    @Column(name = "reading_score")
    Integer readingScore;

    @Column(name = "listening_score")
    Integer listeningScore;

    @Column(name = "total_score")
    Integer totalScore;

    @Column(name = "duration_seconds")
    Integer durationSeconds;

    @Column(name = "selected_part_numbers", length = 50)
    String selectedPartNumbers;

    @OneToMany(mappedBy = "attempt", fetch = FetchType.LAZY)
    Set<UserAnswer> answers;
}
