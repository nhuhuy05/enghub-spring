package com.nhuhuy05.enghub.test.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "test_parts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_test_parts_test_part_number", columnNames = {"test_id", "part_number"}),
                @UniqueConstraint(name = "uq_test_parts_id_test_id", columnNames = {"id", "test_id"})
        }
)
public class TestPart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false)
    Test test;

    @Column(name = "part_number", nullable = false)
    Integer partNumber;

    @Column(nullable = false, length = 255)
    String title;

    @OneToMany(mappedBy = "testPart", fetch = FetchType.LAZY)
    Set<QuestionGroup> questionGroups;
}
