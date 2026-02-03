<script setup>
import { computed } from 'vue'
import { Trophy, StarFilled, SuccessFilled, DataLine, CircleCheckFilled, CircleCloseFilled, WarningFilled } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true
  },
  quizResult: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'reset'])

const resultPercentage = computed(() => {
  if (!props.quizResult?.totalCount) return 0
  return Math.round((props.quizResult.correctCount / props.quizResult.totalCount) * 100)
})

const resultState = computed(() => {
  const score = resultPercentage.value
  if (score === 100) {
    return {
      level: 'perfect',
      title: '完美通关！',
      subtitle: '太棒了！你已经完全掌握了这些知识点',
      icon: Trophy,
      theme: 'gold'
    }
  } else if (score >= 80) {
    return {
      level: 'excellent',
      title: '成绩优异！',
      subtitle: '表现出色，继续保持！',
      icon: StarFilled,
      theme: 'purple'
    }
  } else if (score >= 60) {
    return {
      level: 'good',
      title: '挑战成功！',
      subtitle: '掌握得不错，再接再厉',
      icon: SuccessFilled,
      theme: 'blue'
    }
  } else {
    return {
      level: 'fair',
      title: '继续加油',
      subtitle: '别灰心，复习一下错题再来挑战',
      icon: DataLine,
      theme: 'gray'
    }
  }
})

function handleClose() {
  emit('update:modelValue', false)
}

function handleReset() {
  emit('update:modelValue', false)
  emit('reset')
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :show-close="false"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    width="520px"
    class="quiz-result-modal"
    align-center
    center
    append-to-body
    destroy-on-close
    v-if="quizResult"
  >
    <div class="result-content" :class="resultState.theme">
      <!-- 顶部光效背景 -->
      <div class="result-glow"></div>

      <!-- 核心成绩区 -->
      <div class="score-hero">
        <div class="icon-ring">
          <el-icon class="main-icon"><component :is="resultState.icon" /></el-icon>
          <div class="particles" v-if="resultPercentage >= 80">
            <span v-for="n in 8" :key="n" class="particle"></span>
          </div>
        </div>
        <h2 class="result-title">{{ resultState.title }}</h2>
        <p class="result-subtitle">{{ resultState.subtitle }}</p>

        <div class="score-badge">
          <span class="score-num">{{ resultPercentage }}</span>
          <span class="score-unit">%</span>
        </div>
      </div>

      <!-- 数据统计网格 (Bento Grid) -->
      <div class="stats-grid">
        <div class="stat-card total">
          <div class="stat-icon-bg">
            <el-icon><DataLine /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-label">总题数</span>
            <span class="stat-value">{{ quizResult?.totalCount || 0 }}</span>
          </div>
        </div>

        <div class="stat-card correct">
          <div class="stat-icon-bg">
            <el-icon><CircleCheckFilled /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-label">正确</span>
            <span class="stat-value">{{ quizResult?.correctCount || 0 }}</span>
          </div>
        </div>

        <div class="stat-card incorrect">
          <div class="stat-icon-bg">
            <el-icon><CircleCloseFilled /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-label">错误</span>
            <span class="stat-value">{{ quizResult?.incorrectCount || 0 }}</span>
          </div>
        </div>

        <div class="stat-card unanswered" :class="{ 'has-value': quizResult?.unansweredCount > 0 }">
          <div class="stat-icon-bg">
            <el-icon><WarningFilled /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-label">未作答</span>
            <span class="stat-value">{{ quizResult?.unansweredCount || 0 }}</span>
          </div>
        </div>
      </div>

      <!-- 底部按钮 -->
      <div class="result-actions">
        <el-button
          class="action-btn secondary"
          @click="handleClose"
        >
          返回
        </el-button>

        <el-button
          type="primary"
          class="action-btn primary"
          @click="handleReset"
        >
          再练一次
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
// 答题结果弹窗样式
.result-content {
  position: relative;
  padding: 40px 32px 32px;
  overflow: hidden;
  border-radius: 24px;

  // Background Glow - 更柔和的光效
  .result-glow {
    position: absolute;
    top: -30%;
    left: 50%;
    transform: translateX(-50%);
    width: 120%;
    height: 120%;
    background: radial-gradient(circle at center, var(--theme-color, rgba(255, 255, 255, 0.12)) 0%, transparent 65%);
    opacity: 1;
    pointer-events: none;
    z-index: 0;
  }

  // Theme Colors - 更专业的配色方案（增强对比度）
  &.gold {
    --theme-color: #fbbf24;
    --theme-gradient: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
    --theme-color-rgb: 251, 191, 36;
  }
  &.purple {
    --theme-color: #a78bfa;
    --theme-gradient: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
    --theme-color-rgb: 167, 139, 250;
  }
  &.blue {
    --theme-color: #60a5fa;
    --theme-gradient: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    --theme-color-rgb: 96, 165, 250;
  }
  &.gray {
    --theme-color: #9ca3af;
    --theme-gradient: linear-gradient(135deg, #6b7280 0%, #4b5563 100%);
    --theme-color-rgb: 156, 163, 175;
  }

  // Hero Section - 玻璃质感优化
  .score-hero {
    position: relative;
    z-index: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    margin-bottom: 28px;
    text-align: center;
    width: 100%;

    .icon-ring {
      position: relative;
      width: 88px;
      height: 88px;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.6);
      backdrop-filter: blur(20px) saturate(180%);
      -webkit-backdrop-filter: blur(20px) saturate(180%);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 20px;
      box-shadow:
        0 4px 12px rgba(0, 0, 0, 0.08),
        0 0 0 4px rgba(var(--theme-color-rgb, 96, 165, 250), 0.15);
      border: none;

      .main-icon {
        font-size: 44px;
        color: var(--theme-color);
        filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
        animation: pop-spring 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
      }

      .particles {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        animation: spin-slow 12s linear infinite;

        .particle {
          position: absolute;
          width: 5px;
          height: 5px;
          background: var(--theme-color);
          border-radius: 50%;
          opacity: 0.6;

          &:nth-child(1) { top: 0; left: 50%; }
          &:nth-child(2) { top: 15%; right: 15%; }
          &:nth-child(3) { top: 50%; right: 0; }
          &:nth-child(4) { bottom: 15%; right: 15%; }
          &:nth-child(5) { bottom: 0; left: 50%; }
          &:nth-child(6) { bottom: 15%; left: 15%; }
          &:nth-child(7) { top: 50%; left: 0; }
          &:nth-child(8) { top: 15%; left: 15%; }
        }
      }
    }

    .result-title {
      font-size: 26px;
      font-weight: 700;
      color: var(--el-text-color-primary);
      margin: 0 0 6px;
      letter-spacing: -0.02em;
      line-height: 1.2;
      text-align: center;
      align-self: center;
    }

    .result-subtitle {
      font-size: 14px;
      font-weight: 400;
      color: var(--el-text-color-secondary);
      margin: 0 0 20px;
      text-align: center;
      padding: 0 16px;
      line-height: 1.5;
      align-self: center;
    }

    .score-badge {
      display: inline-flex;
      align-items: baseline;
      padding: 10px 28px;
      background: rgba(255, 255, 255, 0.5);
      backdrop-filter: blur(20px) saturate(180%);
      -webkit-backdrop-filter: blur(20px) saturate(180%);
      border-radius: 999px;
      border: none;
      box-shadow:
        0 2px 8px rgba(0, 0, 0, 0.04),
        inset 0 1px 0 rgba(255, 255, 255, 0.8);

      .score-num {
        font-size: 42px;
        font-weight: 800;
        line-height: 1;
        background: var(--theme-gradient);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
        letter-spacing: -0.02em;
      }

      .score-unit {
        font-size: 18px;
        color: var(--el-text-color-secondary);
        margin-left: 6px;
        font-weight: 600;
      }
    }
  }

  // Bento Grid - 玻璃质感优化
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
    margin-bottom: 28px;

    .stat-card {
      position: relative;
      background: rgba(255, 255, 255, 0.4);
      backdrop-filter: blur(20px) saturate(180%);
      -webkit-backdrop-filter: blur(20px) saturate(180%);
      border: none;
      border-radius: 14px;
      padding: 14px;
      display: flex;
      align-items: center;
      gap: 10px;
      transition: all 0.2s cubic-bezier(0.25, 0.1, 0.25, 1);
      cursor: default;
      box-shadow:
        0 2px 8px rgba(0, 0, 0, 0.04),
        inset 0 1px 0 rgba(255, 255, 255, 0.8);

      &:hover {
        transform: translateY(-1px);
        background: rgba(255, 255, 255, 0.5);
        box-shadow:
          0 4px 12px rgba(0, 0, 0, 0.06),
          inset 0 1px 0 rgba(255, 255, 255, 0.9);
      }

      &:active {
        box-shadow:
          0 2px 6px rgba(0, 0, 0, 0.04),
          inset 0 1px 0 rgba(255, 255, 255, 0.7);
      }

      .stat-icon-bg {
        width: 40px;
        height: 40px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        flex-shrink: 0;
      }

      .stat-info {
        display: flex;
        flex-direction: column;
        flex: 1;
        min-width: 0;

        .stat-label {
          font-size: 11px;
          font-weight: 500;
          color: var(--el-text-color-secondary);
          margin-bottom: 2px;
          text-transform: uppercase;
          letter-spacing: 0.02em;
        }

        .stat-value {
          font-size: 18px;
          font-weight: 700;
          color: var(--el-text-color-primary);
          font-family: var(--el-font-family-no-number);
          line-height: 1.2;
        }
      }

      &.total .stat-icon-bg {
        background: linear-gradient(135deg, rgba(59, 130, 246, 0.15), rgba(59, 130, 246, 0.1));
        color: #3b82f6;
      }
      &.correct .stat-icon-bg {
        background: linear-gradient(135deg, rgba(34, 197, 94, 0.15), rgba(34, 197, 94, 0.1));
        color: #22c55e;
      }
      &.incorrect .stat-icon-bg {
        background: linear-gradient(135deg, rgba(239, 68, 68, 0.15), rgba(239, 68, 68, 0.1));
        color: #ef4444;
      }
      &.unanswered .stat-icon-bg {
        background: rgba(156, 163, 175, 0.1);
        color: var(--el-text-color-placeholder);
      }
      &.unanswered.has-value .stat-icon-bg {
        background: linear-gradient(135deg, rgba(251, 191, 36, 0.15), rgba(251, 191, 36, 0.1));
        color: #fbbf24;
      }
    }
  }

  // Actions - 使用全局 Apple Style 设计
  .result-actions {
    display: flex;
    flex-direction: row; // 横向布局
    justify-content: space-between; // 按钮分布在两端
    gap: 12px;

    .action-btn {
      flex: 1; // 两个按钮等宽
      height: 44px; // 与全局样式保持一致
      font-size: 15px;
      font-weight: 500; // 使用全局样式的 500
      border-radius: 999px; // Apple Style Pill Shape
      border: none;
      cursor: pointer;
      transition: all 0.2s cubic-bezier(0.25, 0.1, 0.25, 1); // 使用全局过渡曲线
      display: inline-flex;
      align-items: center;
      justify-content: center;
      letter-spacing: -0.01em; // 全局样式的字间距

      // 确保图标文字对齐
      .el-icon {
        display: inline-flex;
        align-items: center;
      }

      &.primary {
        background: var(--theme-gradient);
        color: #ffffff;
        text-shadow: 0 1px 3px rgba(0, 0, 0, 0.25), 0 0 1px rgba(0, 0, 0, 0.1);
        font-weight: 600;
        position: relative;

        // 增加背景对比度层
        &::before {
          content: '';
          position: absolute;
          inset: 0;
          background: rgba(0, 0, 0, 0.08);
          border-radius: inherit;
          pointer-events: none;
        }

        // 确保文字在上层
        span, .el-icon {
          position: relative;
          z-index: 1;
        }

        &:hover {
          opacity: 0.95;
          box-shadow: 0 3px 12px rgba(0, 0, 0, 0.15);

          &::before {
            background: rgba(0, 0, 0, 0.05);
          }
        }

        &:active {
          transform: scale(0.97);
        }

        .el-icon {
          color: inherit;
        }
      }

      &.secondary {
        background: rgba(142, 142, 147, 0.12); // Apple System Gray
        color: var(--el-text-color-primary);

        &:hover {
          background: rgba(142, 142, 147, 0.2);
        }

        &:active {
          transform: scale(0.97);
        }

        .el-icon {
          color: inherit;
        }
      }
    }
  }
}

// 深色模式适配 - 玻璃质感优化
html.dark {
  .result-content {
    // 图标环 - 玻璃质感
    .icon-ring {
      background: rgba(60, 60, 62, 0.5) !important;
      backdrop-filter: blur(20px) saturate(180%) !important;
      -webkit-backdrop-filter: blur(20px) saturate(180%) !important;
      box-shadow:
        0 4px 12px rgba(0, 0, 0, 0.3),
        0 0 0 4px rgba(var(--theme-color-rgb, 96, 165, 250), 0.18) !important;
      border: none !important;
    }

    // 分数徽章 - 玻璃质感
    .score-badge {
      background: rgba(60, 60, 62, 0.5) !important;
      backdrop-filter: blur(20px) saturate(180%) !important;
      -webkit-backdrop-filter: blur(20px) saturate(180%) !important;
      border: none !important;
      box-shadow:
        0 2px 8px rgba(0, 0, 0, 0.2),
        inset 0 1px 0 rgba(255, 255, 255, 0.1) !important;
    }

    // 统计卡片 - 玻璃质感
    .stat-card {
      background: rgba(60, 60, 62, 0.4) !important;
      backdrop-filter: blur(20px) saturate(180%) !important;
      -webkit-backdrop-filter: blur(20px) saturate(180%) !important;
      border: none !important;
      box-shadow:
        0 2px 8px rgba(0, 0, 0, 0.2),
        inset 0 1px 0 rgba(255, 255, 255, 0.08) !important;

      &:hover {
        background: rgba(70, 70, 72, 0.45) !important;
        box-shadow:
          0 4px 12px rgba(0, 0, 0, 0.3),
          inset 0 1px 0 rgba(255, 255, 255, 0.12) !important;
      }

      &.total .stat-icon-bg {
        background: linear-gradient(135deg, rgba(59, 130, 246, 0.25), rgba(59, 130, 246, 0.18)) !important;
        color: #60a5fa !important;
      }
      &.correct .stat-icon-bg {
        background: linear-gradient(135deg, rgba(34, 197, 94, 0.25), rgba(34, 197, 94, 0.18)) !important;
        color: #4ade80 !important;
      }
      &.incorrect .stat-icon-bg {
        background: linear-gradient(135deg, rgba(239, 68, 68, 0.25), rgba(239, 68, 68, 0.18)) !important;
        color: #f87171 !important;
      }
      &.unanswered .stat-icon-bg {
        background: rgba(156, 163, 175, 0.2) !important;
        color: rgba(255, 255, 255, 0.5) !important;
      }
      &.unanswered.has-value .stat-icon-bg {
        background: linear-gradient(135deg, rgba(251, 191, 36, 0.25), rgba(251, 191, 36, 0.18)) !important;
        color: #fbbf24 !important;
      }
    }

    // 次要按钮 - 使用全局深色模式样式
    .action-btn.secondary {
      background: rgba(142, 142, 147, 0.25) !important;
      color: #ffffff !important;

      &:hover {
        background: rgba(142, 142, 147, 0.35) !important;
      }

      &:active {
        color: #ffffff !important;
      }
    }
  }
}

// 优化的动画效果 - 更自然的物理效果
@keyframes pop-spring {
  0% {
    transform: scale(0.8);
    opacity: 0;
  }
  50% {
    transform: scale(1.05);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes spin-slow {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>

<style lang="scss">
// 非 scoped 样式：用于 append-to-body 的弹窗
// Apple Style 玻璃质感优化

// 弹窗容器 - 玻璃质感
.quiz-result-modal {
  background: rgba(255, 255, 255, 0.7) !important;
  backdrop-filter: blur(40px) saturate(180%) !important;
  -webkit-backdrop-filter: blur(40px) saturate(180%) !important;
  border: none !important;
  border-radius: 24px !important;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.9) !important;
  position: fixed !important;
  top: 50% !important;
  left: 50% !important;
  transform: translate(-50%, -50%) !important;
  margin: 0 !important;
  max-height: 90vh;
  max-width: 90vw;
  overflow-y: auto;

  .el-dialog__header { display: none; }
  .el-dialog__body { padding: 0 !important; }
}

// 深色模式 - 弹窗容器玻璃质感
html.dark .quiz-result-modal {
  background: rgba(40, 40, 42, 0.6) !important;
  backdrop-filter: blur(40px) saturate(180%) !important;
  -webkit-backdrop-filter: blur(40px) saturate(180%) !important;
  border: none !important;
  box-shadow:
    0 8px 40px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.12) !important;
}

// 响应式优化
@media (max-width: 600px) {
  .quiz-result-modal {
    width: 90vw !important;
    max-width: 480px;
  }
}
</style>
