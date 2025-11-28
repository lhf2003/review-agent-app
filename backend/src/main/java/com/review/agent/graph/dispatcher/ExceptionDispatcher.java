package com.review.agent.graph.dispatcher;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;

/**
 * 目前没啥用，语法需要，否则会报错
 */
public class ExceptionDispatcher implements EdgeAction {
    @Override
    public String apply(OverAllState state) throws Exception {
        return state.value("nextNode").get().toString();
    }
}
