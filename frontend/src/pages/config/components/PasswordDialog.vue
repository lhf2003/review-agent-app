<script setup>
import { watch, ref, computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirm: ''
})

// Dialog visibility
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// Password form bindings
const oldPassword = computed({
  get: () => passwordForm.value.oldPassword,
  set: (val) => passwordForm.value.oldPassword = val
})

const newPassword = computed({
  get: () => passwordForm.value.newPassword,
  set: (val) => passwordForm.value.newPassword = val
})

const confirm = computed({
  get: () => passwordForm.value.confirm,
  set: (val) => passwordForm.value.confirm = val
})

watch(() => props.modelValue, (val) => {
  if (val) {
    passwordForm.value = { oldPassword: '', newPassword: '', confirm: '' }
  }
})

function handleConfirm() {
  emit('confirm', { ...passwordForm.value })
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="修改密码"
    width="400px"
    class="apple-dialog"
    align-center
  >
    <el-form label-position="top">
      <el-form-item label="原密码">
        <el-input v-model="oldPassword" type="password" show-password class="apple-input" />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="newPassword" type="password" show-password class="apple-input" />
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input v-model="confirm" type="password" show-password class="apple-input" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleConfirm">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
:deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--el-text-color-primary);
  padding-bottom: 8px;
}

.apple-input :deep(.el-input__wrapper) {
  box-shadow: none;
  background-color: var(--el-fill-color-light);
  border-radius: 8px;
  padding: 4px 12px;
  transition: all 0.2s;
}

.apple-input :deep(.el-input__wrapper.is-focus) {
  background-color: var(--el-bg-color);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-5);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
