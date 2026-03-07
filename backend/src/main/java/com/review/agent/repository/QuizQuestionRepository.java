package com.review.agent.repository;

import com.review.agent.entity.pojo.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findByQuizId(Long quizId);

    @Query("SELECT e FROM QuizQuestion e WHERE e.quizId = :quizId ORDER BY e.createdTime DESC LIMIT 10")
    List<QuizQuestion> findTop10ByQuizId(@Param("quizId") Long quizId);

    /**
     * 通过测验ID列表查找题目
     *
     * @param quizIds 测验ID列表
     * @return 题目列表
     */
    List<QuizQuestion> findByQuizIdIn(List<Long> quizIds);

    /**
     * 根据题目内容模糊查询ID列表
     *
     * @param keyword 关键词
     * @return 题目ID列表
     */
    @Query("SELECT q.id FROM QuizQuestion q WHERE q.questionText LIKE %:keyword%")
    List<Long> findIdsByQuestionContaining(@Param("keyword") String keyword);

    /**
     * 根据标签ID统计题目数量
     *
     * @param tagId 标签ID
     * @return 题目数量
     */
    long countByTagId(Long tagId);
}
