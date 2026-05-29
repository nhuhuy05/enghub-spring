package com.nhuhuy05.enghub.test.repository;

import com.nhuhuy05.enghub.test.entity.QuestionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface QuestionGroupRepository extends JpaRepository<QuestionGroup, Long> {
    Optional<QuestionGroup> findByTestPartTestIdAndTestPartPartNumberAndOrderIndex(
            Long testId,
            Integer partNumber,
            Integer orderIndex
    );

    List<QuestionGroup> findAllByTestPartTestId(Long testId);

    boolean existsByMediaAssetId(Long mediaAssetId);

    @Query("""
            select count(qg)
            from QuestionGroup qg
            where qg.testPart.test.id = :testId
              and qg.testPart.partNumber = 1
              and qg.mediaAsset is null
            """)
    long countPartOneGroupsWithoutImage(Long testId);
}
