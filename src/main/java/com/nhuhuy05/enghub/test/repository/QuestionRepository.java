package com.nhuhuy05.enghub.test.repository;

import com.nhuhuy05.enghub.test.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findAllByQuestionGroupIdOrderByQuestionNumberAsc(Long questionGroupId);

    @Query("""
            select q
            from Question q
            where q.questionGroup.testPart.test.id = :testId
              and q.questionGroup.testPart.partNumber in :partNumbers
            order by q.questionNumber asc
            """)
    List<Question> findAllByTestIdAndPartNumbersOrderByQuestionNumber(Long testId, List<Integer> partNumbers);

    @Query("""
            select case when count(q) > 0 then true else false end
            from Question q
            where q.id = :questionId
              and q.questionGroup.testPart.test.id = :testId
              and q.questionGroup.testPart.partNumber in :partNumbers
            """)
    boolean existsByIdAndTestIdAndPartNumbers(Long questionId, Long testId, List<Integer> partNumbers);

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
