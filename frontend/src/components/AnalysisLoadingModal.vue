<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true
  },
  fileName: {
    type: String,
    default: ''
  },
  currentStage: {
    type: Number,
    default: 1
  },
  hasError: {
    type: Boolean,
    default: false
  },
  fileId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'close', 'retry'])

// 加载阶段
const currentProgress = ref(0)

// 分析阶段定义 - 三个阶段
const stages = [
  {
    id: 1,
    icon: '📂',
    title: '解析文件',
    description: '正在识别和拆分会话内容...',
    progress: 33
  },
  {
    id: 2,
    icon: '🤖',
    title: 'AI分析中',
    description: '深度分析每个会话...',
    progress: 66
  },
  {
    id: 3,
    icon: '✨',
    title: '分析完成',
    description: '分析结果已生成！',
    progress: 100
  }
]

// 当前阶段信息
const currentStageInfo = computed(() => {
  if (props.hasError) {
    return {
      icon: '❌',
      title: '分析失败',
      description: '分析过程中出现错误'
    }
  }
  return stages.find(s => s.id === props.currentStage) || stages[0]
})

// 进度百分比 (基于阶段)
const progressPercentage = computed(() => {
  if (props.hasError) return currentProgress.value
  if (props.currentStage === 3) return 100
  return stages.find(s => s.id === props.currentStage)?.progress || 0
})

// 动态提示文案
const dynamicTips = computed(() => {
  const baseTips = [
    '💡 AI 正在分析您的对话内容',
    '🔍 提取问题、根因和解决方案',
    '📊 分析完成后可查看详细报告'
  ]

  return baseTips
})

const currentTip = ref('')

// 切换提示文案
let tipInterval = null
const rotateTips = () => {
  // 清除旧的定时器
  if (tipInterval) {
    clearInterval(tipInterval)
  }

  let index = 0
  const tips = dynamicTips.value
  currentTip.value = tips[0]
  tipInterval = setInterval(() => {
    index = (index + 1) % tips.length
    currentTip.value = tips[index]
  }, 8000)
}

onMounted(() => {
  rotateTips()
})

onUnmounted(() => {
  if (tipInterval) clearInterval(tipInterval)
})

// 监听阶段变化，检测完成
watch(() => props.currentStage, (newStage) => {
  if (newStage === 3) {
    // 阶段3 = 完成
    currentProgress.value = 100
    setTimeout(() => {
      handleClose()
    }, 1500) // 延迟1.5秒后关闭
  }
})

// 监听弹窗打开，重置进度
watch(() => props.modelValue, (isVisible) => {
  if (isVisible) {
    // 重置进度
    currentProgress.value = 0
    // 重新启动提示文案轮播
    rotateTips()
  } else {
    // 弹窗关闭时，清除定时器
    if (tipInterval) {
      clearInterval(tipInterval)
      tipInterval = null
    }
  }
})

// 处理关闭
function handleClose() {
  emit('update:modelValue', false)
  emit('close')
}

// 处理重试
function handleRetry() {
  emit('retry', props.fileId)
}
</script>

<template>
  <teleport to="body">
    <transition name="fade">
      <div v-if="modelValue" class="loading-overlay">
        <div class="loading-container">
          <!-- 顶部装饰光效 -->
          <div class="glow-effect"></div>

          <!-- 主要内容 -->
          <div class="loading-content">
            <!-- 动画图标 -->
            <div class="animation-wrapper">
              <div class="pulse-ring"></div>
              <div class="pulse-ring delay-1"></div>
              <div class="pulse-ring delay-2"></div>
              <div class="center-icon">{{ currentStageInfo.icon }}</div>
            </div>

            <!-- 当前阶段 -->
            <div class="stage-info">
              <h3 class="stage-title">{{ currentStageInfo.title }}</h3>
              <p class="stage-description">{{ currentStageInfo.description }}</p>
              <p v-if="fileName" class="file-name">{{ fileName }}</p>
            </div>

            <!-- 进度条 -->
            <div class="progress-section" v-if="!hasError">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: `${progressPercentage}%` }">
                  <div class="progress-shine"></div>
                </div>
              </div>
              <div class="progress-text">
                <span class="time-elapsed">阶段 {{ currentStage }}/3</span>
                <span v-if="currentStage < 3" class="time-estimate">进行中...</span>
                <span v-else class="time-completed">分析完成</span>
              </div>
            </div>

            <!-- 动态提示 -->
            <div class="tip-section" v-if="!hasError">
              <transition name="tip-fade" mode="out-in">
                <p :key="currentTip" class="tip-text">{{ currentTip }}</p>
              </transition>
            </div>

            <!-- 错误操作按钮 -->
            <div class="error-actions" v-if="hasError">
              <el-button type="primary" size="large" @click="handleRetry">
                重新分析
              </el-button>
              <el-button size="large" @click="handleClose">
                关闭
              </el-button>
            </div>

            <!-- 教育价值说明 -->
            <div class="value-section" v-if="!hasError">
              <p class="value-text">
                📖 正在分析您的对话记录
              </p>
            </div>
          </div>

          <!-- 底部装饰 -->
          <div class="bottom-decoration">
            <div class="dot-dot"></div>
            <div class="dot-dot"></div>
            <div class="dot-dot"></div>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<style scoped lang="scss">
// 加载遮罩层
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 20px;
}

// 加载容器
.loading-container {
  position: relative;
  width: 100%;
  max-width: 480px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(40px) saturate(180%);
  -webkit-backdrop-filter: blur(40px) saturate(180%);
  border-radius: 24px;
  padding: 48px 40px 40px;
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.8),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  overflow: hidden;
}

// 顶部光效
.glow-effect {
  position: absolute;
  top: -50%;
  left: 50%;
  transform: translateX(-50%);
  width: 200%;
  height: 100%;
  background: radial-gradient(
    circle at center,
    rgba(99, 102, 241, 0.15) 0%,
    transparent 60%
  );
  pointer-events: none;
  animation: glow-pulse 3s ease-in-out infinite;
}

// 主要内容
.loading-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

// 动画包裹器
.animation-wrapper {
  position: relative;
  width: 120px;
  height: 120px;
  margin-bottom: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

// 脉冲环
.pulse-ring {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 2px solid rgba(99, 102, 241, 0.3);
  animation: pulse-scale 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;

  &.delay-1 {
    animation-delay: 0.4s;
  }

  &.delay-2 {
    animation-delay: 0.8s;
  }
}

// 中心图标
.center-icon {
  font-size: 48px;
  animation: icon-bounce 2s ease-in-out infinite;
  filter: drop-shadow(0 4px 8px rgba(99, 102, 241, 0.2));
}

// 阶段信息
.stage-info {
  margin-bottom: 32px;

  .stage-title {
    font-size: 22px;
    font-weight: 700;
    color: var(--el-text-color-primary);
    margin: 0 0 12px;
    letter-spacing: -0.02em;
  }

  .stage-description {
    font-size: 15px;
    font-weight: 400;
    color: var(--el-text-color-secondary);
    margin: 0 0 8px;
    line-height: 1.6;
  }

  .file-name {
    font-size: 13px;
    font-weight: 500;
    color: var(--el-color-primary);
    margin: 0;
    opacity: 0.8;
  }
}

// 进度条区域
.progress-section {
  width: 100%;
  margin-bottom: 24px;

  .progress-bar {
    position: relative;
    width: 100%;
    height: 8px;
    background: rgba(0, 0, 0, 0.06);
    border-radius: 999px;
    overflow: hidden;
    margin-bottom: 12px;
  }

  .progress-fill {
    position: relative;
    height: 100%;
    background: linear-gradient(90deg, #6366f1 0%, #8b5cf6 50%, #6366f1 100%);
    background-size: 200% 100%;
    border-radius: 999px;
    transition: width 0.5s cubic-bezier(0.4, 0, 0.2, 1);
    animation: gradient-shift 2s ease-in-out infinite;
    overflow: hidden;

    .progress-shine {
      position: absolute;
      top: 0;
      left: 0;
      width: 50%;
      height: 100%;
      background: linear-gradient(
        90deg,
        transparent 0%,
        rgba(255, 255, 255, 0.4) 50%,
        transparent 100%
      );
      animation: shine-move 1.5s ease-in-out infinite;
    }
  }

  .progress-text {
    display: flex;
    justify-content: space-between;
    font-size: 13px;
    color: var(--el-text-color-secondary);

    .time-elapsed {
      font-weight: 500;
    }

    .time-estimate {
      font-weight: 400;
      opacity: 0.8;
    }
  }
}

// 提示区域
.tip-section {
  width: 100%;
  margin-bottom: 20px;
  min-height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;

  .tip-text {
    font-size: 14px;
    color: var(--el-text-color-regular);
    line-height: 1.5;
    margin: 0;
    padding: 10px 16px;
    background: rgba(99, 102, 241, 0.08);
    border-radius: 12px;
  }
}

// 错误操作按钮
.error-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

// 价值说明
.value-section {
  width: 100%;

  .value-text {
    font-size: 13px;
    color: var(--el-text-color-secondary);
    margin: 0;
    opacity: 0.7;
  }
}

// 底部装饰
.bottom-decoration {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;

  .dot-dot {
    width: 8px;
    height: 8px;
    background: rgba(99, 102, 241, 0.3);
    border-radius: 50%;
    animation: dot-bounce 1.4s ease-in-out infinite;

    &:nth-child(2) {
      animation-delay: 0.2s;
    }

    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }
}

// 动画定义
@keyframes pulse-scale {
  0%, 100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  50% {
    transform: scale(1);
    opacity: 0;
  }
}

@keyframes icon-bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

@keyframes gradient-shift {
  0%, 100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

@keyframes shine-move {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

@keyframes glow-pulse {
  0%, 100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}

@keyframes dot-bounce {
  0%, 80%, 100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

// 过渡动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.tip-fade-enter-active,
.tip-fade-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.tip-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.tip-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

// 深色模式适配
html.dark {
  .loading-container {
    background: rgba(40, 40, 42, 0.75);
    box-shadow:
      0 20px 60px rgba(0, 0, 0, 0.5),
      0 0 0 1px rgba(255, 255, 255, 0.1),
      inset 0 1px 0 rgba(255, 255, 255, 0.1);
  }

  .glow-effect {
    background: radial-gradient(
      circle at center,
      rgba(139, 92, 246, 0.2) 0%,
      transparent 60%
    );
  }

  .pulse-ring {
    border-color: rgba(139, 92, 246, 0.4);
  }

  .progress-bar {
    background: rgba(255, 255, 255, 0.1);
  }

  .tip-section .tip-text {
    background: rgba(139, 92, 246, 0.15);
  }

  .bottom-decoration .dot-dot {
    background: rgba(139, 92, 246, 0.4);
  }
}

// 响应式设计
@media (max-width: 600px) {
  .loading-container {
    padding: 40px 28px 32px;
    margin: 20px;
  }

  .animation-wrapper {
    width: 100px;
    height: 100px;
    margin-bottom: 24px;
  }

  .center-icon {
    font-size: 40px;
  }

  .stage-info {
    .stage-title {
      font-size: 20px;
    }

    .stage-description {
      font-size: 14px;
    }
  }
}
</style>
