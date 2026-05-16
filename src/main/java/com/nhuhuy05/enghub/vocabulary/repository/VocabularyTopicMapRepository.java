package com.nhuhuy05.enghub.vocabulary.repository;

import com.nhuhuy05.enghub.vocabulary.entity.VocabularyTopicMap;
import com.nhuhuy05.enghub.vocabulary.entity.VocabularyTopicMapId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VocabularyTopicMapRepository extends JpaRepository<VocabularyTopicMap, VocabularyTopicMapId> {
}

