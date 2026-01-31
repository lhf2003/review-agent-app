<script setup>
defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  passwordForm: {
    type: Object,
    required: true
  }
})

defineEmits(['update:visible', 'changePassword'])

function handleClose() {
  emit('update:visible', false)
}

function changePassword() {
  emit('changePassword')
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="emit('update:visible', $event)"
    title="修改密码"
    width="400px"
    :close-on-click-modal="false"
    @close="$emit('update:visible', false)"
  >
    <el-form label-width="80px">
      <el-form-item label="原密码">
        <el-input
          v-model="passwordForm.oldPassword"
          type="password"
          show-password
          placeholder="请输入原密码"
        />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input
          v-model="passwordForm.newPassword"
          type="password"
          show-password
          placeholder="请输入新密码"
        />
      </el-form-item>
      <el-form-item label="确认密码">
        <el-input
          v-model="passwordForm.confirm"
          type="password"
          show-password
          placeholder="请再次输入新密码"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="changePassword">确认修改</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
/* Password Dialog uses Element Plus default styles */
</style>
