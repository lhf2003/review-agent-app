<script setup>
import { RefreshRight, ArrowLeft } from '@element-plus/icons-vue'

defineProps({
  collectionName: {
    type: String,
    default: ''
  }
})

defineEmits(['back', 'reset'])
</script>

<template>
  <div class="nav-header">
    <div class="header-left">
      <el-button
        link
        class="back-btn"
        @click="$emit('back')"
      >
        <el-icon><ArrowLeft /></el-icon>
        <span class="back-text">返回合集</span>
      </el-button>
      <div class="divider"></div>
      <h1 class="header-title">AI 学习辅导</h1>
      <span class="collection-name" v-if="collectionName">
        {{ collectionName }}
      </span>
    </div>

    <div class="header-right">
      <el-button
        type="primary"
        plain
        round
        :icon="RefreshRight"
        @click="$emit('reset')"
        class="reset-btn"
      >
        重新开始
      </el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.nav-header {
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--el-bg-color-page);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--el-border-color-light);
  flex-shrink: 0;
  z-index: 100;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .back-btn {
      font-size: 15px;
      font-weight: 500;
      color: var(--el-text-color-regular);
      padding: 0;
      height: auto;

      &:hover {
        color: var(--el-color-primary);
      }

      .el-icon {
        margin-right: 4px;
        font-size: 18px;
      }
    }

    .divider {
      width: 1px;
      height: 24px;
      background-color: var(--el-border-color-light);
    }

    .header-title {
      font-size: 18px;
      font-weight: 700;
      margin: 0;
      color: var(--el-text-color-primary);
    }

    .collection-name {
      font-size: 14px;
      color: var(--el-text-color-secondary);
      background: var(--el-fill-color-light);
      padding: 4px 12px;
      border-radius: 12px;
    }
  }
}

// Dark Mode Adaptation
:global(.dark) {
  .nav-header {
    background: rgba(28, 28, 30, 0.75); // Modern Glass Dark
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }
}

// Responsive Design
@media (max-width: 480px) {
  .nav-header {
    padding: 0 16px;

    .header-left {
      gap: 12px;

      .back-text {
        display: none; // Hide text on very small screens
      }
    }

    .header-right {
      .reset-btn {
        padding: 8px 12px;
        font-size: 14px;
      }
    }
  }
}
</style>
