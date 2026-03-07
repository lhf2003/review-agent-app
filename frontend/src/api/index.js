/**
 * API 模块统一入口
 * 提供所有 API 模块的统一导出
 */

// 基础工具
export { getToken, getUserId, request, handleStream, BASE_URL } from './base'

// 各模块 API
export { authApi } from './auth'
export { tagApi } from './tag'
export { tagRelationApi } from './tagRelation'
export { learningPathApi } from './learningPath'
export { dataApi } from './data'
export { analysisApi } from './analysis'
export { collectionApi } from './collection'
export { quizApi } from './quiz'
export { llmApi } from './llm'
export { statisticApi } from './statistic'
export { notificationApi } from './notification'
export { exportApi } from './export'
export { knowledgeGraphApi } from './knowledgeGraph'

// 默认导出各模块
import authApi from './auth'
import tagApi from './tag'
import tagRelationApi from './tagRelation'
import learningPathApi from './learningPath'
import dataApi from './data'
import analysisApi from './analysis'
import collectionApi from './collection'
import quizApi from './quiz'
import llmApi from './llm'
import statisticApi from './statistic'
import notificationApi from './notification'
import exportApi from './export'

export default {
  auth: authApi,
  tag: tagApi,
  tagRelation: tagRelationApi,
  learningPath: learningPathApi,
  data: dataApi,
  analysis: analysisApi,
  collection: collectionApi,
  quiz: quizApi,
  llm: llmApi,
  statistic: statisticApi,
  notification: notificationApi,
  export: exportApi
}
