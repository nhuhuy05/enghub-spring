package com.nhuhuy05.enghub.test.repository;

import com.nhuhuy05.enghub.test.entity.TestCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestCollectionRepository extends JpaRepository<TestCollection, Long> {
    boolean existsByName(String name);

    Optional<TestCollection> findByName(String name);

    @Query("""
            select distinct collection
            from TestCollection collection
            join collection.tests test
            where test.published = true
            order by collection.name asc
            """)
    List<TestCollection> findCollectionsWithPublishedTests();
}
