package com.review.agent.config;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.review.agent.graph.nodes.DataAnalysisNode;
import com.review.agent.graph.nodes.TagClassifyNode;
import com.review.agent.graph.nodes.ThinkingParadigmNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.alibaba.cloud.ai.graph.OverAllState.DEFAULT_INPUT_KEY;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 工作流配置
 */
@Slf4j
@Configuration
public class GraphConfig {
    @Resource
    private DataAnalysisNode dataAnalysisNode;

    @Resource
    private TagClassifyNode tagClassifyNode;

    @Resource
    private ThinkingParadigmNode thinkingParadigmNode;

    @Resource
    private SaverConfig saverConfig;

    @Bean
    public KeyStrategyFactory keyStrategyFactory() {
        return new KeyStrategyFactoryBuilder().addStrategy(DEFAULT_INPUT_KEY, KeyStrategy.REPLACE)
                .addStrategy("file_id", KeyStrategy.REPLACE) // 文件id
                .addStrategy("user_id", KeyStrategy.REPLACE) // 用户id
                .build();
    }

    /**
     * 分析工作流
     */
    @Bean
    public StateGraph analysisGraph(KeyStrategyFactory keyStrategyFactory) throws GraphStateException {

        return new StateGraph("Review Agent Workflow", keyStrategyFactory)
                // 添加节点
                .addNode("analysis_agent", node_async(dataAnalysisNode))
                .addNode("tag_classify_agent", node_async(tagClassifyNode))
                .addNode("thinking_paradigm_agent", node_async(thinkingParadigmNode))
                // 定义边
                .addEdge(START, "tag_classify_agent")
                .addEdge("tag_classify_agent", "thinking_paradigm_agent")
                .addEdge("thinking_paradigm_agent", "analysis_agent")
                .addEdge("analysis_agent", END);
    }

    /**
     * 报告工作流
     */
    @Bean
    public StateGraph reportGraph(KeyStrategyFactory keyStrategyFactory) throws GraphStateException {

        return new StateGraph("Review Agent Workflow", keyStrategyFactory)
                // 添加节点
                .addNode("analysis_agent", node_async(dataAnalysisNode))
                .addNode("tag_classify_agent", node_async(tagClassifyNode))
                // 定义边
                .addEdge(START, "analysis_agent")
                .addEdge("analysis_agent", "tag_classify_agent")
                .addEdge("tag_classify_agent", END);
    }

    @Bean(name = "analysisCompiledGraph")
    public CompiledGraph analysisCompiledGraph(@Qualifier("analysisGraph") StateGraph stateGraph) throws GraphStateException {

        CompiledGraph compiledGraph = stateGraph.compile(
                CompileConfig.builder()
                        .saverConfig(saverConfig)
                        .build());
        // 配置定时任务，每15分钟执行一次
//        compiledGraph.schedule(ScheduleConfig.builder().cronExpression("0 0/5 * * * ?").build());
        return compiledGraph;
    }

    @Bean(name = "reportCompiledGraph")
    public CompiledGraph reportCompiledGraph(@Qualifier("reportGraph") StateGraph stateGraph) throws GraphStateException {
        return stateGraph.compile(
                CompileConfig.builder()
                        .saverConfig(saverConfig)
                        .build());
    }

}
