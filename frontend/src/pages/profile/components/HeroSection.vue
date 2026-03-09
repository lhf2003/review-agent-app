<script setup>
import { Edit as EditIcon } from '@element-plus/icons-vue'

defineProps({
  userInfo: {
    type: Object,
    required: true
  },
  animatedStats: {
    type: Object,
    required: true
  },
  showContent: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['edit'])
</script>

<template>
  <div class="hero-section" :class="{ visible: showContent }">
    <div class="hero-content">
      <div class="hero-avatar-wrapper">
        <el-avatar :size="100" :src="userInfo.avatar">
          {{ userInfo.username?.[0]?.toUpperCase() }}
        </el-avatar>
        <div class="avatar-glow"></div>
      </div>
      <div class="hero-text">
        <h1 class="hero-title">
          欢迎回来，{{ userInfo.username }}
        </h1>
        <p class="hero-subtitle">
          今天是陪伴你的第 <span class="highlight">{{ animatedStats.learningDays }}</span> 天
        </p>
      </div>
      <button class="edit-profile-btn" @click="emit('edit')">
        <el-icon><EditIcon /></el-icon>
        <span>编辑资料</span>
      </button>
    </div>
    <div class="hero-pattern"></div>
  </div>
</template>

<style scoped>
/* Hero Section */
.hero-section {
  position: relative;
  padding: 48px 40px;
  border-radius: 24px;
  background: linear-gradient(135deg,
    var(--el-color-primary-light-9) 0%,
    var(--el-color-primary-light-7) 100%
  );
  margin-bottom: 32px;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 32px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.1);
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.hero-section.visible {
  opacity: 1;
  transform: translateY(0);
}

.hero-pattern {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle, rgba(255, 255, 255, 0.1) 1px, transparent 1px);
  background-size: 24px 24px;
  animation: floatPattern 20s linear infinite;
  pointer-events: none;
}

@keyframes floatPattern {
  0% { transform: translate(0, 0); }
  100% { transform: translate(24px, 24px); }
}

.hero-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 32px;
  flex: 1;
}

.hero-avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.avatar-glow {
  position: absolute;
  inset: -8px;
  background: var(--el-color-primary);
  opacity: 0.15;
  border-radius: 50%;
  filter: blur(20px);
  animation: pulseGlow 3s ease-in-out infinite;
}

@keyframes pulseGlow {
  0%, 100% { transform: scale(1); opacity: 0.15; }
  50% { transform: scale(1.1); opacity: 0.25; }
}

.hero-text {
  flex: 1;
}

.hero-title {
  margin: 0 0 12px 0;
  font-size: 32px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1.2;
}

.hero-subtitle {
  margin: 0;
  font-size: 16px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.hero-subtitle .highlight {
  color: var(--el-color-primary);
  font-weight: 600;
  font-size: 20px;
}

.edit-profile-btn {
  padding: 10px 20px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: var(--el-text-color-primary);
  font-weight: 500;
  transition: all 0.3s ease;
  flex-shrink: 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.edit-profile-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

</style>
