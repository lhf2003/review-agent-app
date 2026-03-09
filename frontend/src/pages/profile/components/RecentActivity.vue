<script setup>
import { Document, FolderOpened, Edit, Notebook } from '@element-plus/icons-vue'

const props = defineProps({
  recentActivities: {
    type: Array,
    required: true
  },
  cardsVisible: {
    type: Array,
    required: true
  }
})

const getActivityIcon = (type) => {
  const icons = {
    sync: Document,
    collection: FolderOpened,
    quiz: Edit,
    report: Notebook
  }
  return icons[type] || Document
}
</script>

<template>
  <div class="activity-section" :class="{ visible: cardsVisible[6] }">
    <el-timeline v-if="recentActivities && recentActivities.length > 0">
      <el-timeline-item
        v-for="(activity, index) in recentActivities"
        :key="index"
        :timestamp="activity.time"
        placement="top"
      >
        <div class="activity-item">
          <el-icon class="activity-icon">
            <component :is="getActivityIcon(activity.type)" />
          </el-icon>
          <span class="activity-detail">{{ activity.detail }}</span>
        </div>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-else description="暂无最近活动" />
  </div>
</template>

<style scoped>
/* Activity Section */
.activity-section {
  padding: 32px;
  background: rgba(255, 255, 255, 0.75);
  border-radius: 20px;
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.3) inset;
  border: none;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s ease;
}

.activity-section.visible {
  opacity: 1;
  transform: translateY(0);
}

/* el-timeline 样式定制 */
.activity-section :deep(.el-timeline) {
  padding-left: 0;
}

.activity-section :deep(.el-timeline-item) {
  padding-bottom: 20px;
}

.activity-section :deep(.el-timeline-item__wrapper) {
  padding-left: 0;
  padding-right: 0;
}

.activity-section :deep(.el-timeline-item__tail) {
  left: 10px;
  border-left: 2px solid var(--el-border-color-light);
}

.activity-section :deep(.el-timeline-item__node--normal) {
  left: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-color-primary);
  border: 3px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.activity-section :deep(.el-timeline-item__node .el-icon) {
  color: white;
  font-size: 12px;
}

.activity-section :deep(.el-timeline-item__timestamp) {
  position: absolute;
  left: 28px;
  top: 0;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
  line-height: 20px;
  padding: 0 8px;
  white-space: nowrap;
}

.activity-section :deep(.el-timeline-item__content) {
  padding-left: 32px;
  padding-right: 0;
  padding-top: 24px;
  padding-bottom: 0;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  transition: all 0.3s ease;
}

.activity-item:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: translateX(4px);
}

.activity-icon {
  font-size: 20px;
  color: var(--el-color-primary);
}

.activity-detail {
  font-size: 14px;
  color: var(--el-text-color-primary);
  font-weight: 500;
  line-height: 1.5;
}

/* el-empty 样式定制 */
.activity-section :deep(.el-empty) {
  padding: 40px 0;
}

.activity-section :deep(.el-empty__description) {
  color: var(--el-text-color-secondary);
}
</style>
