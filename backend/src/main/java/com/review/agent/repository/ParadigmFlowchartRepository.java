package com.review.agent.repository;

import com.review.agent.entity.pojo.ParadigmFlowchart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 思维范式流程图Repository
 */
@Repository
public interface ParadigmFlowchartRepository extends JpaRepository<ParadigmFlowchart, Long> {

    /**
     * 根据分析结果ID查询流程图列表
     */
    List<ParadigmFlowchart> findByAnalysisResultId(Long analysisResultId);

    /**
     * 根据用户ID和范式编码查询流程图列表
     */
    List<ParadigmFlowchart> findByUserIdAndParadigmCode(Long userId, String paradigmCode);

    /**
     * 根据ID和用户ID查询流程图
     */
    Optional<ParadigmFlowchart> findByIdAndUserId(Long id, Long userId);
}
