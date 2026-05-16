package com.nhuhuy05.enghub.vocabulary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class VocabularyTopicMapId implements Serializable {
    @Column(name = "vocabulary_id")
    Long vocabularyId;

    @Column(name = "topic_id")
    Long topicId;
}

