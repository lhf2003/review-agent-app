package com.review.agent.repository;

import com.review.agent.entity.pojo.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findByQuizId(Long quizId);
}
