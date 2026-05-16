package com.nhuhuy05.enghub.notification.repository;

import com.nhuhuy05.enghub.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
