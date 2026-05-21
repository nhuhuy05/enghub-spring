package com.nhuhuy05.enghub.test.repository;

import com.nhuhuy05.enghub.test.entity.TestPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestPartRepository extends JpaRepository<TestPart, Long> {
    boolean existsByTestIdAndPartNumber(Long testId, Integer partNumber);

    List<TestPart> findAllByTestIdOrderByPartNumberAsc(Long testId);

    Optional<TestPart> findByTestIdAndPartNumber(Long testId, Integer partNumber);
}
