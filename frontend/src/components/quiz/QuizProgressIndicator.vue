<script setup>
defineProps({
  current: {
    type: Number,
    required: true
  },
  total: {
    type: Number,
    required: true
  },
  percentage: {
    type: Number,
    required: true
  }
})
</script>

<template>
  <div class="vertical-progress">
    <div class="progress-indicator">
      <div class="progress-number">{{ current }}</div>
      <div class="progress-divider">/</div>
      <div class="progress-total">{{ total }}</div>
    </div>
    <div class="progress-bar-vertical">
      <div
        class="progress-fill-vertical"
        :style="{ height: `${percentage}%` }"
      ></div>
    </div>
  </div>
</template>

<style scoped lang="scss">
// 右侧垂直进度指示器
.vertical-progress {
  position: fixed;
  right: 32px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 50;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;

  .progress-indicator {
    display: flex;
    align-items: baseline;
    gap: 4px;
    font-weight: 700;
    font-size: 24px;
    color: var(--el-text-color-primary);
    font-variant-numeric: tabular-nums;

    .progress-number {
      font-size: 32px;
      color: var(--el-color-primary);
    }

    .progress-divider {
      font-size: 20px;
      color: var(--el-text-color-secondary);
      opacity: 0.6;
    }

    .progress-total {
      font-size: 18px;
      color: var(--el-text-color-secondary);
      opacity: 0.8;
    }
  }

  .progress-bar-vertical {
    width: 6px;
    height: 200px;
    background: var(--el-fill-color-darker);
    border-radius: 3px;
    overflow: hidden;
    position: relative;
  }

  .progress-fill-vertical {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    background: linear-gradient(180deg, var(--el-color-primary-light-3), var(--el-color-primary));
    border-radius: 3px;
    transition: height 0.5s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 0 0 10px rgba(var(--el-color-primary-rgb), 0.3);
  }
}

// Dark Mode Adaptation
:global(.dark) {
  .vertical-progress {
    .progress-indicator {
      color: var(--el-text-color-primary);
    }

    .progress-bar-vertical {
      background: rgba(255, 255, 255, 0.1);
    }
  }
}

// Responsive Design
@media (max-width: 1024px) {
  .vertical-progress {
    right: 24px;
  }
}

@media (max-width: 768px) {
  .vertical-progress {
    right: 16px;

    .progress-indicator {
      font-size: 18px;

      .progress-number {
        font-size: 24px;
      }

      .progress-divider {
        font-size: 16px;
      }

      .progress-total {
        font-size: 14px;
      }
    }

    .progress-bar-vertical {
      height: 150px;
    }
  }
}

@media (max-width: 480px) {
  .vertical-progress {
    right: 12px;
    gap: 12px;

    .progress-indicator {
      font-size: 16px;

      .progress-number {
        font-size: 20px;
      }

      .progress-divider {
        font-size: 14px;
      }

      .progress-total {
        font-size: 12px;
      }
    }

    .progress-bar-vertical {
      width: 4px;
      height: 120px;
    }
  }
}
</style>
