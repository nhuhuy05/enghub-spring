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
public class VocabularyTestMapId implements Serializable {
    @Column(name = "vocabulary_id")
    Long vocabularyId;

    @Column(name = "test_part_id")
    Long testPartId;
}

