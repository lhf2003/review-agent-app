<script setup>
import { ArrowLeft, ArrowRight, Aim, SuccessFilled } from '@element-plus/icons-vue'

defineProps({
  currentIndex: {
    type: Number,
    required: true
  },
  totalQuestions: {
    type: Number,
    required: true
  },
  isAllSubmitted: {
    type: Boolean,
    default: false
  },
  canGoPrev: {
    type: Boolean,
    default: true
  },
  canGoNext: {
    type: Boolean,
    default: true
  }
})

defineEmits(['prev', 'next', 'submit'])
</script>

<template>
  <div class="quiz-footer">
    <el-button
      :disabled="!canGoPrev"
      @click="$emit('prev')"
      size="large"
      class="nav-btn prev-btn"
      :icon="ArrowLeft"
      round
    >
      上一题
    </el-button>

    <!-- 提交答案按钮（仅在最后一题且未提交时显示） -->
    <el-button
      v-if="currentIndex === totalQuestions - 1 && !isAllSubmitted"
      type="success"
      @click="$emit('submit')"
      size="large"
      class="nav-btn submit-btn"
      round
    >
      <el-icon class="el-icon--left"><Aim /></el-icon>
      提交答案
    </el-button>

    <!-- 下一题按钮（仅在非最后一题时显示） -->
    <el-button
      v-if="!isAllSubmitted && currentIndex < totalQuestions - 1"
      type="primary"
      :disabled="!canGoNext"
      @click="$emit('next')"
      size="large"
      class="nav-btn next-btn"
      round
    >
      下一题
      <el-icon class="el-icon--right"><ArrowRight /></el-icon>
    </el-button>

    <!-- 已提交后的完成提示 -->
    <div v-if="isAllSubmitted" class="completion-badge">
      <el-icon color="#67c23a"><SuccessFilled /></el-icon>
      <span>已完成答题</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.quiz-footer {
  display: flex;
  justify-content: center;
  gap: 24px;
  padding: 20px 0 0 0;
  flex-shrink: 0; // Prevent footer from shrinking
  align-items: center;

  .nav-btn {
    min-width: 160px;
    height: 52px;
    font-size: 16px;
    font-weight: 600;
    border-radius: 26px; // Pill shape
  }

  :deep(.nav-btn.prev-btn) {
    background: rgba(255, 255, 255, 0.05) !important;
    border: 1px solid rgba(255, 255, 255, 0.1) !important;
    color: var(--text-primary) !important;

    &:hover:not(:disabled) {
      background: rgba(255, 255, 255, 0.08) !important;
      border-color: rgba(204, 102, 51, 0.5) !important;
      box-shadow: 0 0 16px rgba(204, 102, 51, 0.3);
    }
  }

  :deep(.nav-btn.next-btn) {
    background: linear-gradient(135deg, #CC6633 0%, #E07B47 100%) !important;
    border: none !important;
    box-shadow: 0 4px 16px rgba(204, 102, 51, 0.4);

    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 6px 24px rgba(204, 102, 51, 0.55);
    }

    &:active:not(:disabled) {
      transform: scale(0.98);
    }
  }

  :deep(.nav-btn.submit-btn) {
    background: linear-gradient(135deg, #CC6633 0%, #E07B47 100%) !important;
    border: none !important;
    box-shadow: 0 4px 16px rgba(204, 102, 51, 0.4);

    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 6px 24px rgba(204, 102, 51, 0.55);
    }

    &:active:not(:disabled) {
      transform: scale(0.98);
    }
  }

  .completion-badge {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 24px;
    background: rgba(204, 102, 51, 0.1);
    border: 1px solid rgba(204, 102, 51, 0.4);
    border-radius: 26px;
    font-size: 16px;
    font-weight: 600;
    color: var(--accent-secondary);

    .el-icon {
      font-size: 20px;
      color: var(--accent-primary);
    }
  }
}

// Responsive Design
@media (max-width: 768px) {
  .quiz-footer {
    .nav-btn {
      min-width: 120px;
      height: 48px;
      font-size: 15px;
    }
  }
}

@media (max-width: 480px) {
  .quiz-footer {
    gap: 16px;

    .nav-btn {
      min-width: 100px;
      height: 44px;
      font-size: 14px;
      padding: 0 16px;
    }
  }
}
</style>
