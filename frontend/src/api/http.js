/**
 * API 兼容层
 * 保持向后兼容性，将所有 API 方法汇总到一个对象中
 *
 * 推荐使用方式：
 * 1. 按模块导入（推荐）：
 *    import { quizApi } from '@/api'
 *    quizApi.getQuizHistory()
 *
 * 2. 兼容旧代码：
 *    import { api } from '@/api/http'
 *    api.getQuizHistory()
 */

import {
  getToken,
  getUserId,
  request,
  handleStream,
  BASE_URL,
  encryptPassword
} from './base'

import { authApi } from './auth'
import { tagApi } from './tag'
import { dataApi } from './data'
import { analysisApi } from './analysis'
import { collectionApi } from './collection'
import { quizApi } from './quiz'
import { llmApi } from './llm'
import { statisticApi } from './statistic'
import { notificationApi } from './notification'
import { exportApi } from './export'

/**
 * 兼容旧的 api 对象
 * 将所有模块的方法合并到一个对象中
 */
export const api = {
  // ========== 认证相关 ==========
  login: authApi.login.bind(authApi),
  register: authApi.register.bind(authApi),
  logout: authApi.logout,
  getConfig: authApi.getConfig.bind(authApi),
  updateConfig: authApi.updateConfig.bind(authApi),
  getUserModelConfig: authApi.getUserModelConfig.bind(authApi),
  updateUserModelConfig: authApi.updateUserModelConfig.bind(authApi),
  getUserInfo: authApi.getUserInfo.bind(authApi),
  updateUserInfo: authApi.updateUserInfo.bind(authApi),
  updateUserPassword: authApi.updateUserPassword.bind(authApi),
  getUserStats: authApi.getUserStats.bind(authApi),

  // ========== 标签相关 ==========
  getMainTagList: tagApi.getMainTagList.bind(tagApi),
  getTagRelations: tagApi.getTagRelations.bind(tagApi),
  addMainTag: tagApi.addMainTag.bind(tagApi),
  updateMainTag: tagApi.updateMainTag.bind(tagApi),
  deleteMainTag: tagApi.deleteMainTag.bind(tagApi),
  getSubTagList: tagApi.getSubTagList.bind(tagApi),
  addSubTag: tagApi.addSubTag.bind(tagApi),
  updateSubTag: tagApi.updateSubTag.bind(tagApi),
  deleteSubTag: tagApi.deleteSubTag.bind(tagApi),
  addTagRelation: tagApi.addTagRelation.bind(tagApi),
  deleteTagRelation: tagApi.deleteTagRelation.bind(tagApi),

  // ========== 数据和文件相关 ==========
  importFile: dataApi.importFile.bind(dataApi),
  updateFileStatus: dataApi.updateFileStatus.bind(dataApi),
  getSyncHistory: dataApi.getSyncHistory.bind(dataApi),
  dataPage: dataApi.dataPage.bind(dataApi),
  dataCreate: dataApi.dataCreate.bind(dataApi),
  dataImport: dataApi.dataImport.bind(dataApi),
  dataUpdateStatus: dataApi.dataUpdateStatus.bind(dataApi),
  dataDelete: dataApi.dataDelete.bind(dataApi),

  // ========== 分析相关 ==========
  getAnalysisList: analysisApi.getAnalysisList.bind(analysisApi),
  getAnalysisByFile: analysisApi.getAnalysisByFile.bind(analysisApi),
  startAnalysis: analysisApi.startAnalysis.bind(analysisApi),
  getAnalysisResult: analysisApi.getAnalysisResult.bind(analysisApi),
  getAnalysisResultByIds: analysisApi.getAnalysisResultByIds.bind(analysisApi),
  getSessionTrace: analysisApi.getSessionTrace.bind(analysisApi),
  getSimilarIssues: analysisApi.getSimilarIssues.bind(analysisApi),
  analysisLogStream: analysisApi.analysisLogStream.bind(analysisApi),

  // ========== 合集相关 ==========
  getCollectionList: collectionApi.getCollectionList.bind(collectionApi),
  createCollection: collectionApi.createCollection.bind(collectionApi),
  updateCollection: collectionApi.updateCollection.bind(collectionApi),
  deleteCollection: collectionApi.deleteCollection.bind(collectionApi),
  getCollectionDetail: collectionApi.getCollectionDetail.bind(collectionApi),
  addSessionToCollection: collectionApi.addSessionToCollection.bind(collectionApi),
  removeSessionFromCollection: collectionApi.removeSessionFromCollection.bind(collectionApi),
  generateQuiz: collectionApi.generateQuiz.bind(collectionApi),
  getCollectionRecommendations: collectionApi.getCollectionRecommendations.bind(collectionApi),
  getSmartRecommendations: collectionApi.getSmartRecommendations.bind(collectionApi),
  quickCreateCollection: collectionApi.quickCreateCollection.bind(collectionApi),
  dismissRecommendation: collectionApi.dismissRecommendation.bind(collectionApi),

  // ========== 测验和错题相关 ==========
  submitAnswer: quizApi.submitAnswer.bind(quizApi),
  submitBatchAnswers: quizApi.submitBatchAnswers.bind(quizApi),
  resetQuiz: quizApi.resetQuiz.bind(quizApi),
  getQuizHistory: quizApi.getQuizHistory.bind(quizApi),
  getQuizDetail: quizApi.getQuizDetail.bind(quizApi),
  checkQuizVersion: quizApi.checkQuizVersion.bind(quizApi),
  regenerateQuiz: quizApi.regenerateQuiz.bind(quizApi),
  getQuizStats: quizApi.getQuizStats.bind(quizApi),
  getKnowledgeMastery: quizApi.getKnowledgeMastery.bind(quizApi),
  getMistakeList: quizApi.getMistakeList.bind(quizApi),
  getMistakeStats: quizApi.getMistakeStats.bind(quizApi),
  getReviewRecommendation: quizApi.getReviewRecommendation.bind(quizApi),
  markMistakesMastered: quizApi.markMistakesMastered.bind(quizApi),
  deleteMistakes: quizApi.deleteMistakes.bind(quizApi),
  getDashboard: quizApi.getDashboard.bind(quizApi),
  getDashboardTrend: quizApi.getDashboardTrend.bind(quizApi),

  // ========== LLM 相关 ==========
  connectLlmProvider: llmApi.connectLlmProvider.bind(llmApi),
  getLlmModels: llmApi.getLlmModels.bind(llmApi),
  getSelectedModelList: llmApi.getSelectedModelList.bind(llmApi),
  activeSelectedModel: llmApi.activeSelectedModel.bind(llmApi),
  deactiveSelectedModel: llmApi.deactiveSelectedModel.bind(llmApi),
  getUserDefaultModels: llmApi.getUserDefaultModels.bind(llmApi),
  updateUserDefaultModels: llmApi.updateUserDefaultModels.bind(llmApi),
  getChatPlaceholders: llmApi.getChatPlaceholders.bind(llmApi),
  chatStream: llmApi.chatStream.bind(llmApi),
  chatWithAnalysisStream: llmApi.chatWithAnalysisStream.bind(llmApi),

  // ========== 统计报表相关 ==========
  getWordReport: statisticApi.getWordReport.bind(statisticApi),
  getDateTagCountTrend: statisticApi.getDateTagCountTrend.bind(statisticApi),
  getReportList: statisticApi.getReportList.bind(statisticApi),

  // ========== 导出相关 ==========
  exportAnalysis: exportApi.exportAnalysis.bind(exportApi),
  exportBatchAnalysis: exportApi.exportBatchAnalysis.bind(exportApi),
  exportCollection: exportApi.exportCollection.bind(exportApi),
  downloadBlob: exportApi.downloadBlob,

  // ========== 通知相关 ==========
  getNotificationSettings: notificationApi.getNotificationSettings.bind(notificationApi),
  updateNotificationSettings: notificationApi.updateNotificationSettings.bind(notificationApi),
  getPendingReviews: notificationApi.getPendingReviews.bind(notificationApi),
  getPendingReviewStats: notificationApi.getPendingReviewStats.bind(notificationApi),

  // ========== 流式处理 ==========
  _handleStream: handleStream
}

// 导出工具函数供直接使用
export { getToken, getUserId, request, handleStream, BASE_URL, encryptPassword }

// 默认导出 api 对象
export default api
