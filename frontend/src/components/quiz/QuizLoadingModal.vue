<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true
  },
  collectionName: {
    type: String,
    default: ''
  },
  estimatedSeconds: {
    type: Number,
    default: 100
  },
  apiCompleted: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'completed'])

// 加载阶段
const currentStage = ref(0)
const elapsedSeconds = ref(0)
const isAccelerating = ref(false)
const currentProgress = ref(0) // 记录当前实际进度，确保只增不减

// 加载阶段定义 - 模拟 AI 生成流程
const stages = [
  {
    icon: '📚',
    title: '分析学习内容',
    description: '正在深入理解您的知识库...'
  },
  {
    icon: '🧠',
    title: '智能生成试题',
    description: '根据您的学习重点精心设计...'
  },
  {
    icon: '✨',
    title: '优化题目质量',
    description: '确保题目准确且有挑战性...'
  },
  {
    icon: '🎯',
    title: '准备完成',
    description: '马上就可以开始学习了！'
  }
]

// 当前阶段信息
const currentStageInfo = computed(() => stages[currentStage.value])

// 进度百分比 (基于估算时间)
const progressPercentage = computed(() => {
  // 如果 API 已完成，返回 100%
  if (props.apiCompleted) {
    return 100
  }

  // 基于传入的预估时间计算目标进度
  const totalStages = stages.length
  const secondsPerStage = props.estimatedSeconds / totalStages
  const stageProgress = (elapsedSeconds.value % secondsPerStage) / secondsPerStage
  const targetProgress = Math.min(((currentStage.value + stageProgress) / totalStages) * 100, 95)

  // 更新当前进度（确保只增不减）
  currentProgress.value = Math.max(currentProgress.value, targetProgress)

  return currentProgress.value
})

// 动态提示文案
const dynamicTips = computed(() => {
  const baseTips = [
    '💡 AI 正在为您定制专属学习内容',
    '🎓 试题生成后，您可以反复练习巩固'
  ]

  const timeTips = props.estimatedSeconds < 30
    ? [
        '⏱️ 预计约 ' + props.estimatedSeconds + ' 秒完成',
        '💡 AI 正在为您定制专属学习内容'
      ]
    : props.estimatedSeconds < 60
    ? [
        '⏱️ 预计约 ' + Math.floor(props.estimatedSeconds / 10) * 10 + '-' + Math.ceil(props.estimatedSeconds / 10) * 10 + ' 秒',
        '💡 AI 正在为您定制专属学习内容'
      ]
    : [
        '⏱️ 内容较多，请耐心等待约 ' + Math.floor(props.estimatedSeconds / 60) + ' 分钟',
        '🌟 生成完成后可立即开始练习'
      ]

  return [...baseTips, ...timeTips]
})

const currentTip = ref('')

// 切换提示文案
const rotateTips = () => {
  let index = 0
  const tips = dynamicTips.value
  currentTip.value = tips[0]
  setInterval(() => {
    index = (index + 1) % tips.length
    currentTip.value = tips[index]
  }, 8000)
}

// 更新阶段
const updateStage = () => {
  // 每隔固定时间切换阶段（每8秒或按预估时间平均分配）
  const secondsPerStage = Math.max(Math.floor(props.estimatedSeconds / stages.length), 6)

  const stageInterval = setInterval(() => {
    const maxStage = stages.length - 1
    if (currentStage.value < maxStage) {
      currentStage.value++
    } else {
      clearInterval(stageInterval)
    }
  }, secondsPerStage * 1000)

  // 记录定时器以便清理
  return stageInterval
}

// 更新计时器
const updateTimer = () => {
  const timer = setInterval(() => {
    elapsedSeconds.value++
  }, 1000)
  return timer
}

let stageInterval
let timer

onMounted(() => {
  rotateTips()
  stageInterval = updateStage()
  timer = updateTimer()
})

onUnmounted(() => {
  if (stageInterval) clearInterval(stageInterval)
  if (timer) clearInterval(timer)
})

// 监听 API 完成状态，加速进度条
watch(() => props.apiCompleted, (completed) => {
  if (completed && !isAccelerating.value) {
    isAccelerating.value = true
    // 快速跳转到最后一阶段
    currentStage.value = stages.length - 1

    // 延迟一小段时间后 emit completed 事件
    setTimeout(() => {
      emit('completed')
    }, 1200) // 增加延迟到 1.2 秒，让用户看到 "准备完成" 阶段
  }
})

// 监听弹窗打开，重置进度
watch(() => props.modelValue, (isVisible) => {
  if (isVisible) {
    // 重置所有状态
    currentProgress.value = 0
    currentStage.value = 0
    elapsedSeconds.value = 0
    isAccelerating.value = false
    // 重新设置阶段切换定时器
    if (stageInterval) clearInterval(stageInterval)
    stageInterval = updateStage()
  }
})

// 监听预估时间变化（当父组件更新时间时）
watch(() => props.estimatedSeconds, (newTime) => {
  // 当预估时间更新且弹窗正在显示时，重置阶段切换
  if (props.modelValue && stageInterval) {
    clearInterval(stageInterval)
    stageInterval = updateStage()
  }
})
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
              <h3 class="stage-title">{{ apiCompleted ? '准备完成' : currentStageInfo.title }}</h3>
              <p class="stage-description">{{ apiCompleted ? '马上就可以开始学习了！' : currentStageInfo.description }}</p>
            </div>

            <!-- 进度条 -->
            <div class="progress-section">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: `${progressPercentage}%` }">
                  <div class="progress-shine"></div>
                </div>
              </div>
              <div class="progress-text">
                <span class="time-elapsed">已等待 {{ elapsedSeconds }} 秒</span>
                <span v-if="!apiCompleted" class="time-estimate">预计还需 {{ Math.max(0, estimatedSeconds - elapsedSeconds) }} 秒</span>
                <span v-else class="time-completed">已完成生成</span>
              </div>
            </div>

            <!-- 动态提示 -->
            <div class="tip-section">
              <transition name="tip-fade" mode="out-in">
                <p :key="currentTip" class="tip-text">{{ currentTip }}</p>
              </transition>
            </div>

            <!-- 教育价值说明 -->
            <div class="value-section">
              <p class="value-text">
                📖 正在分析 {{ collectionName || '您的知识库' }} 中的 {{ Math.floor(Math.random() * 20 + 10) }} 个知识点
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

// 加载容器 - Warm Amber 深色主题
.loading-container {
  position: relative;
  width: 100%;
  max-width: 480px;
  background: var(--glass-surface);
  backdrop-filter: blur(var(--glass-blur-strong)) saturate(180%);
  -webkit-backdrop-filter: blur(var(--glass-blur-strong)) saturate(180%);
  border-radius: var(--radius-card);
  padding: 48px 40px 40px;
  border: 1px solid var(--glass-border);
  border-top: 1px solid var(--glass-highlight);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}

// 顶部光效 - Warm Amber 主题
.glow-effect {
  position: absolute;
  top: -50%;
  left: 50%;
  transform: translateX(-50%);
  width: 200%;
  height: 100%;
  background: radial-gradient(
    circle at center,
    rgba(204, 102, 51, 0.2) 0%,
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

// 脉冲环 - Warm Amber 主题
.pulse-ring {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 2px solid rgba(204, 102, 51, 0.3);
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
  filter: drop-shadow(0 4px 8px rgba(204, 102, 51, 0.3));
}

// 阶段信息
.stage-info {
  margin-bottom: 32px;

  .stage-title {
    font-size: 22px;
    font-weight: 700;
    color: var(--text-primary);
    margin: 0 0 12px;
    letter-spacing: -0.02em;
  }

  .stage-description {
    font-size: 15px;
    font-weight: 400;
    color: var(--text-secondary);
    margin: 0;
    line-height: 1.6;
  }
}

// 进度条区域 - Warm Amber 主题
.progress-section {
  width: 100%;
  margin-bottom: 24px;

  .progress-bar {
    position: relative;
    width: 100%;
    height: 8px;
    background: rgba(255, 248, 245, 0.1);
    border-radius: 999px;
    overflow: hidden;
    margin-bottom: 12px;
  }

  .progress-fill {
    position: relative;
    height: 100%;
    background: linear-gradient(90deg, var(--accent-primary) 0%, var(--accent-secondary) 50%, var(--accent-primary) 100%);
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
        rgba(255, 255, 255, 0.3) 50%,
        transparent 100%
      );
      animation: shine-move 1.5s ease-in-out infinite;
    }
  }

  .progress-text {
    display: flex;
    justify-content: space-between;
    font-size: 13px;
    color: var(--text-secondary);

    .time-elapsed {
      font-weight: 500;
    }

    .time-estimate {
      font-weight: 400;
      opacity: 0.8;
    }
  }
}

// 提示区域 - Warm Amber 主题
.tip-section {
  width: 100%;
  margin-bottom: 20px;
  min-height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;

  .tip-text {
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.5;
    margin: 0;
    padding: 10px 16px;
    background: rgba(204, 102, 51, 0.1);
    border-radius: 12px;
    border: 1px solid rgba(204, 102, 51, 0.15);
  }
}

// 价值说明
.value-section {
  width: 100%;

  .value-text {
    font-size: 13px;
    color: var(--text-tertiary);
    margin: 0;
    opacity: 0.8;
  }
}

// 底部装饰 - Warm Amber 主题
.bottom-decoration {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;

  .dot-dot {
    width: 8px;
    height: 8px;
    background: rgba(204, 102, 51, 0.4);
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
    transform: translateX(100%); /* 移动一个自身宽度到右侧边缘 */
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
