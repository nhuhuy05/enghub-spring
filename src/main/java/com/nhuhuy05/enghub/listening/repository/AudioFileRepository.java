package com.nhuhuy05.enghub.listening.repository;

import com.nhuhuy05.enghub.listening.entity.AudioFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AudioFileRepository extends JpaRepository<AudioFile, Long> {
}

