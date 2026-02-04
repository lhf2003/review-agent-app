<script setup>
import { computed } from 'vue'
import { Refresh } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Array,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'refresh'])

const shortcuts = [
  {
    text: '最近一周',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
      return [start, end]
    }
  },
  {
    text: '最近一个月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
      return [start, end]
    }
  },
  {
    text: '最近三个月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
      return [start, end]
    }
  }
]

function handleDateChange() {
  emit('change')
}

function handleRefresh() {
  emit('refresh')
}
</script>

<template>
  <div class="date-range-filter">
    <el-date-picker
      :model-value="modelValue"
      @update:model-value="(val) => emit('update:modelValue', val)"
      type="daterange"
      range-separator="至"
      start-placeholder="开始日期"
      end-placeholder="结束日期"
      :shortcuts="shortcuts"
      @change="handleDateChange"
      style="width: 100%"
    />
    <el-button
      type="plain"
      link
      size="large"
      @click="handleRefresh"
      :loading="loading"
    >
      <el-icon v-if="!loading"><Refresh /></el-icon>
    </el-button>
  </div>
</template>

<style scoped>
.date-range-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  max-width: 400px;
}
</style>
