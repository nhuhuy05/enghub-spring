package com.nhuhuy05.enghub.test.repository;

import com.nhuhuy05.enghub.test.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findAllByQuestionIdOrderByIdAsc(Long questionId);
}
