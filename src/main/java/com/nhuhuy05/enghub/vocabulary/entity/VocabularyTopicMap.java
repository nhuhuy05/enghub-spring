package com.nhuhuy05.enghub.vocabulary.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "vocabulary_topic_map")
public class VocabularyTopicMap {
    @EmbeddedId
    VocabularyTopicMapId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("vocabularyId")
    @JoinColumn(name = "vocabulary_id", nullable = false)
    Vocabulary vocabulary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("topicId")
    @JoinColumn(name = "topic_id", nullable = false)
    VocabularyTopic topic;
}

