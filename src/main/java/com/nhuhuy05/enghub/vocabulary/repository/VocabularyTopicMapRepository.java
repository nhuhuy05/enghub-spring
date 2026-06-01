package com.nhuhuy05.enghub.vocabulary.repository;

import com.nhuhuy05.enghub.vocabulary.entity.VocabularyTopicMap;
import com.nhuhuy05.enghub.vocabulary.entity.VocabularyTopicMapId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VocabularyTopicMapRepository extends JpaRepository<VocabularyTopicMap, VocabularyTopicMapId> {
    @Query("""
            select topicMap
            from VocabularyTopicMap topicMap
            where topicMap.vocabulary.id = :vocabularyId
            order by topicMap.topic.name asc
            """)
    List<VocabularyTopicMap> findAllByVocabularyId(Long vocabularyId);

    @Query("""
            select topicMap
            from VocabularyTopicMap topicMap
            where topicMap.vocabulary.id in :vocabularyIds
            order by topicMap.topic.name asc
            """)
    List<VocabularyTopicMap> findAllByVocabularyIdIn(List<Long> vocabularyIds);

    @Query("""
            select count(topicMap)
            from VocabularyTopicMap topicMap
            where topicMap.topic.id = :topicId
            """)
    long countByTopicId(Long topicId);
}
