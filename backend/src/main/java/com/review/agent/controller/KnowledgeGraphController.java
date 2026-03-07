package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.vo.SimpleKnowledgeGraphVO;
import com.review.agent.service.KnowledgeGraphService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识图谱控制器
 */
@Slf4j
@RestController
@RequestMapping("/knowledge-graph")
public class KnowledgeGraphController {

    @Resource
    private KnowledgeGraphService knowledgeGraphService;

    @Resource
    private SecurityUtils securityUtils;

    /**
     * 获取简化版知识图谱数据
     *
     * @return 知识图谱数据（节点和边）
     */
    @GetMapping("/simple")
    public BaseResponse<SimpleKnowledgeGraphVO> getSimpleKnowledgeGraph() {
        Long userId = securityUtils.getCurrentUserId();
        log.debug("Getting simple knowledge graph for user: {}", userId);

        SimpleKnowledgeGraphVO result = knowledgeGraphService.getSimpleKnowledgeGraph(userId);
        return ResultUtil.success(result);
    }
}
