<script setup>
import { ref, computed } from 'vue'
import { Trophy, Star, ArrowRight, Check } from '@element-plus/icons-vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  achievements: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close'])

// 当前显示的成就索引
const currentIndex = ref(0)

// 当前成就
const currentAchievement = computed(() => {
  return props.achievements[currentIndex.value] || null
})

// 是否有多个成就
const hasMultiple = computed(() => {
  return props.achievements.length > 1
})

// 是否是最后一个
const isLast = computed(() => {
  return currentIndex.value >= props.achievements.length - 1
})

// 下一个
function next() {
  if (!isLast.value) {
    currentIndex.value++
  } else {
    emit('close')
  }
}

// 关闭
function handleClose() {
  currentIndex.value = 0
  emit('close')
}
</script>

<template>
  <teleport to="body">
    <transition name="fade">
      <div v-if="visible" class="achievement-overlay" @click.self="handleClose">
        <transition name="spring" appear>
          <div v-if="currentAchievement" class="achievement-card">
            <!-- 光效背景 -->
            <div class="glow-effect"></div>
            
            <!-- 内容区域 -->
            <div class="card-content">
              <!-- 图标区 -->
              <div class="icon-section">
                <div class="icon-ring">
                  <div class="icon-circle">
                    <el-icon :size="40" class="trophy-icon"><Trophy /></el-icon>
                  </div>
                </div>
                <div class="confetti-burst"></div>
              </div>

              <!-- 文本区 -->
              <div class="text-section">
                <div class="badge">
                  <el-icon :size="12"><Star /></el-icon>
                  <span>成就解锁</span>
                </div>
                <h2 class="title">{{ currentAchievement.name }}</h2>
                <p class="description">{{ currentAchievement.description }}</p>
              </div>

              <!-- 进度条 (如果有进度信息) -->
              <div class="progress-section">
                <div class="progress-bar-bg">
                  <div class="progress-bar-fill"></div>
                </div>
                <div class="progress-label">目标达成 100%</div>
              </div>

              <!-- 底部操作区 -->
              <div class="actions">
                <button 
                  class="action-btn primary"
                  @click="hasMultiple ? next() : handleClose()"
                >
                  <span>{{ hasMultiple && !isLast ? '下一个' : '太棒了' }}</span>
                  <el-icon v-if="hasMultiple && !isLast" class="btn-icon"><ArrowRight /></el-icon>
                  <el-icon v-else class="btn-icon"><Check /></el-icon>
                </button>
              </div>

              <!-- 分页指示器 -->
              <div v-if="hasMultiple" class="pagination">
                <span 
                  v-for="(_, index) in achievements" 
                  :key="index"
                  class="dot"
                  :class="{ active: index === currentIndex }"
                ></span>
              </div>
            </div>
          </div>
        </transition>
      </div>
    </transition>
  </teleport>
</template>

<style scoped lang="scss">
// 变量定义
$color-primary: #0D9488; // Teal
$color-accent: #F59E0B; // Amber/Gold
$ease-spring: cubic-bezier(0.175, 0.885, 0.32, 1.275);
$ease-apple: cubic-bezier(0.25, 1, 0.5, 1);

.achievement-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.3); // 更通透的背景
  backdrop-filter: blur(8px);
  perspective: 1000px;
}

.achievement-card {
  position: relative;
  width: 90%;
  max-width: 380px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: saturate(180%) blur(20px);
  border-radius: 32px;
  box-shadow: 
    0 25px 50px -12px rgba(0, 0, 0, 0.25),
    0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  overflow: hidden;
  text-align: center;
  transform-origin: center center;
  
  // 深色模式适配
  :deep(.dark) & {
    background: rgba(30, 30, 30, 0.85);
    box-shadow: 
      0 25px 50px -12px rgba(0, 0, 0, 0.5),
      0 0 0 1px rgba(255, 255, 255, 0.1) inset;
  }
}

.glow-effect {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(
    circle at center,
    rgba($color-accent, 0.15) 0%,
    transparent 60%
  );
  animation: rotate 10s linear infinite;
  pointer-events: none;
}

.card-content {
  position: relative;
  z-index: 1;
  padding: 40px 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

// 图标区域
.icon-section {
  position: relative;
  margin-bottom: 24px;
}

.icon-ring {
  position: relative;
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: linear-gradient(135deg, #FFF 0%, #F0F0F0 100%);
  box-shadow: 
    0 10px 20px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(255, 255, 255, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: float 3s ease-in-out infinite;

  :deep(.dark) & {
    background: linear-gradient(135deg, #444 0%, #2a2a2a 100%);
    box-shadow: 
      0 10px 20px rgba(0, 0, 0, 0.3),
      0 0 0 1px rgba(255, 255, 255, 0.1);
  }
}

.icon-circle {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at 30% 30%, #FFED4A 0%, #FFD700 100%);
  mask-image: radial-gradient(white, black); // Fix for some browsers
  -webkit-mask-image: -webkit-radial-gradient(white, black);
  
  // 让内部变成金色渐变，图标白色
  .trophy-icon {
    color: #92400E; // Dark gold/brown for contrast
    filter: drop-shadow(0 2px 4px rgba(0,0,0,0.1));
  }
}

// 文本区域
.badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: rgba($color-accent, 0.1);
  color: #D97706; // Darker Amber
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 16px;
  
  :deep(.dark) & {
    background: rgba($color-accent, 0.2);
    color: #FCD34D;
  }
}

.title {
  font-size: 24px;
  font-weight: 800;
  color: #111827;
  margin: 0 0 8px;
  letter-spacing: -0.02em;
  
  :deep(.dark) & {
    color: #F9FAFB;
  }
}

.description {
  font-size: 15px;
  color: #6B7280;
  line-height: 1.5;
  margin: 0 0 24px;
  max-width: 280px;
  
  :deep(.dark) & {
    color: #9CA3AF;
  }
}

// 进度条
.progress-section {
  width: 100%;
  margin-bottom: 32px;
}

.progress-bar-bg {
  height: 6px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 8px;
  
  :deep(.dark) & {
    background: rgba(255, 255, 255, 0.1);
  }
}

.progress-bar-fill {
  height: 100%;
  width: 100%; // 默认满
  background: linear-gradient(90deg, $color-accent, #FBBF24);
  border-radius: 3px;
  transform-origin: left;
  animation: fillProgress 1s $ease-apple;
}

.progress-label {
  font-size: 12px;
  color: $color-accent;
  font-weight: 600;
}

// 按钮
.action-btn {
  width: 100%;
  height: 48px;
  border-radius: 24px;
  border: none;
  background: #111827;
  color: white;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
  }

  &:active {
    transform: translateY(0);
  }

  :deep(.dark) & {
    background: #F9FAFB;
    color: #111827;
    
    &:hover {
      background: #FFF;
    }
  }
}

// 分页点
.pagination {
  display: flex;
  gap: 6px;
  margin-top: 24px;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;

  :deep(.dark) & {
    background: rgba(255, 255, 255, 0.2);
  }

  &.active {
    background: $color-accent;
    transform: scale(1.2);
  }
}

// 动画定义
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes fillProgress {
  from { transform: scaleX(0); }
  to { transform: scaleX(1); }
}

// Transition Groups
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.spring-enter-active {
  transition: all 0.6s $ease-spring;
}
.spring-leave-active {
  transition: all 0.3s ease-in;
}
.spring-enter-from {
  opacity: 0;
  transform: scale(0.8) translateY(20px);
}
.spring-leave-to {
  opacity: 0;
  transform: scale(0.9) translateY(-20px);
}
.spring-enter-to,
.spring-leave-from {
  opacity: 1;
  transform: scale(1) translateY(0);
}
</style>
