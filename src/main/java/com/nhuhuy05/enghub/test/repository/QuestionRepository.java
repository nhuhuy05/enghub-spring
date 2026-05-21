package com.nhuhuy05.enghub.test.repository;

import com.nhuhuy05.enghub.test.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Query("""
            select count(q)
            from Question q
            where q.questionGroup.testPart.test.id = :testId
            """)
    long countByTestId(Long testId);

    @Query("""
            select count(q)
            from Question q
            where q.questionGroup.testPart.test.id = :testId
              and (
                  select count(a)
                  from Answer a
                  where a.question = q and a.isCorrect = true
              ) <> 1
            """)
    long countQuestionsWithoutExactlyOneCorrectAnswer(Long testId);
}
