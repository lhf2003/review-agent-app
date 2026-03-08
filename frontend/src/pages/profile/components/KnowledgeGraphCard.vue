<script setup>
import { useRouter } from 'vue-router'
import { Share } from '@element-plus/icons-vue'

const router = useRouter()

function goToKnowledgeGraph() {
  router.push('/knowledge-graph')
}
</script>

<template>
  <div class="knowledge-graph-card" @click="goToKnowledgeGraph">
    <div class="card-content">
      <div class="icon-wrapper">
        <el-icon :size="32"><Share /></el-icon>
      </div>
      <div class="text-content">
        <h3 class="title">知识图谱</h3>
        <p class="description">可视化你的知识掌握情况，发现薄弱环节</p>
      </div>
      <el-icon class="arrow-icon" :size="20"><ArrowRight /></el-icon>
    </div>

    <!-- 预览示意 -->
    <div class="preview-dots">
      <span class="dot weak"></span>
      <span class="dot medium"></span>
      <span class="dot strong"></span>
      <span class="dot weak"></span>
      <span class="dot strong"></span>
    </div>
  </div>
</template>

<script>
import { ArrowRight } from '@element-plus/icons-vue'
export default {
  components: { ArrowRight }
}
</script>

<style scoped>
.knowledge-graph-card {
  background: linear-gradient(135deg, rgba(24, 144, 255, 0.1), rgba(82, 196, 26, 0.05));
  border: 1px solid rgba(24, 144, 255, 0.2);
  border-radius: 16px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.knowledge-graph-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(24, 144, 255, 0.15);
  border-color: rgba(24, 144, 255, 0.4);
}

.card-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.icon-wrapper {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #1890ff, #36cfc9);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.text-content {
  flex: 1;
}

.title {
  font-size: 17px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 6px 0;
}

.description {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin: 0;
  line-height: 1.5;
}

.arrow-icon {
  color: var(--el-text-color-placeholder);
  transition: transform 0.2s ease;
}

.knowledge-graph-card:hover .arrow-icon {
  transform: translateX(4px);
  color: #1890ff;
}

/* 预览小点 */
.preview-dots {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed rgba(0, 0, 0, 0.1);
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
}

.dot.weak {
  background: #ff4d4f;
  animation-delay: 0s;
}

.dot.medium {
  background: #faad14;
  animation-delay: 0.4s;
}

.dot.strong {
  background: #52c41a;
  animation-delay: 0.8s;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(1.1); }
}

html.dark .preview-dots {
  border-top-color: rgba(255, 255, 255, 0.1);
}
</style>
