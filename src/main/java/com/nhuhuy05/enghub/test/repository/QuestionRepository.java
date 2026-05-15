package com.nhuhuy05.enghub.test.repository;

import com.nhuhuy05.enghub.test.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
}
