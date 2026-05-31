package com.nhuhuy05.enghub.test.repository;

import com.nhuhuy05.enghub.test.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {
    boolean existsByCollectionIdAndTestNumber(Long collectionId, Integer testNumber);

    List<Test> findAllByCollectionIdOrderByTestNumberAsc(Long collectionId);

    List<Test> findAllByPublishedTrueOrderByCreatedAtDesc();

    List<Test> findAllByCollectionIdAndPublishedTrueOrderByTestNumberAsc(Long collectionId);

    Optional<Test> findByIdAndPublishedTrue(Long id);
}
