package com.nhuhuy05.enghub.notification.entity;

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
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false, length = 100)
    String type;

    @Column(nullable = false, length = 255)
    String title;

    @Column(name = "question_number")
    Integer questionNumber;

    String body;

    @Column(name = "is_read", nullable = false)
    boolean isRead;

    @Column(name = "scheduled_at")
    LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    LocalDateTime sentAt;
}

