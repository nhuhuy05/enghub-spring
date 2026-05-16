package com.nhuhuy05.enghub.progress.repository;

import com.nhuhuy05.enghub.progress.entity.UserDailyStreak;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDailyStreakRepository extends JpaRepository<UserDailyStreak, Long> {
}
