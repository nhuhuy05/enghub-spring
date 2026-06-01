package com.nhuhuy05.enghub.vocabulary.repository;

import com.nhuhuy05.enghub.vocabulary.entity.VocabularyTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VocabularyTopicRepository extends JpaRepository<VocabularyTopic, Long> {
    boolean existsByNameIgnoreCase(String name);

    Optional<VocabularyTopic> findByNameIgnoreCase(String name);
}
