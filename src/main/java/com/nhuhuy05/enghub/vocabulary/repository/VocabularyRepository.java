package com.nhuhuy05.enghub.vocabulary.repository;

import com.nhuhuy05.enghub.vocabulary.entity.Vocabulary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {
    boolean existsByWordIgnoreCase(String word);

    Optional<Vocabulary> findByWordIgnoreCase(String word);

    @Query("""
            select vocabulary
            from Vocabulary vocabulary
            where (:keyword is null
                or lower(vocabulary.word) like lower(concat('%', :keyword, '%'))
                or lower(vocabulary.meaningVi) like lower(concat('%', :keyword, '%'))
                or lower(vocabulary.meaningEn) like lower(concat('%', :keyword, '%')))
              and (:topicId is null or exists (
                  select 1
                  from VocabularyTopicMap topicMap
                  where topicMap.vocabulary = vocabulary
                    and topicMap.topic.id = :topicId
              ))
            order by vocabulary.word asc
            """)
    List<Vocabulary> search(Long topicId, String keyword);
}
