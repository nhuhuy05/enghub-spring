package com.nhuhuy05.enghub.vocabulary.entity;

import com.nhuhuy05.enghub.test.entity.TestPart;
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
@Table(name = "vocabulary_test_map")
public class VocabularyTestMap {
    @EmbeddedId
    VocabularyTestMapId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("vocabularyId")
    @JoinColumn(name = "vocabulary_id", nullable = false)
    Vocabulary vocabulary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("testPartId")
    @JoinColumn(name = "test_part_id", nullable = false)
    TestPart testPart;
}

