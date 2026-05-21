package com.nhuhuy05.enghub.test.repository;

import com.nhuhuy05.enghub.test.entity.TestCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestCollectionRepository extends JpaRepository<TestCollection, Long> {
    boolean existsByName(String name);

    Optional<TestCollection> findByName(String name);
}
