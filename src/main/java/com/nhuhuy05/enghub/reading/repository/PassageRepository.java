package com.nhuhuy05.enghub.reading.repository;

import com.nhuhuy05.enghub.reading.entity.Passage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassageRepository extends JpaRepository<Passage, Long> {
}
