<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, Close, CameraFilled, Lock } from '@element-plus/icons-vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  userInfo: {
    type: Object,
    required: true
  },
  userInfoForm: {
    type: Object,
    required: true
  },
  editingField: {
    type: String,
    default: null
  }
})

const emit = defineEmits(['update:visible', 'editField', 'cancelEdit', 'saveUserInfo', 'handleAvatarUpload', 'changePassword'])

const uploadRef = ref(null)

watch(() => props.visible, (val) => {
  if (val) {
    editField(null)
  }
})

function editField(field) {
  emit('editField', field)
}

function cancelEdit() {
  emit('cancelEdit')
}

function saveUserInfo() {
  emit('saveUserInfo')
}

function handleAvatarUpload(file) {
  emit('handleAvatarUpload', file)
}

function beforeAvatarUpload(file) {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

function handleChangePassword() {
  emit('changePassword')
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="emit('update:visible', $event)"
    width="560px"
    :close-on-click-modal="false"
    class="apple-dialog"
    :show-close="false"
  >
    <template #header>
      <div class="dialog-header">
        <span class="dialog-title">编辑资料</span>
        <el-icon class="close-icon" @click="emit('update:visible', false)">
          <Close />
        </el-icon>
      </div>
    </template>

    <div class="profile-edit-content">
      <!-- 第一部分：头像 + 用户信息 -->
      <div class="profile-section section-basic">
        <!-- 左侧：头像 -->
        <div class="avatar-wrapper">
          <el-upload
            ref="uploadRef"
            :show-file-list="false"
            :before-upload="beforeAvatarUpload"
            :on-change="handleAvatarUpload"
            :auto-upload="false"
            accept="image/*"
          >
            <div class="avatar-upload-trigger">
              <el-avatar :size="100" :src="userInfo.avatar" class="user-avatar">
                {{ userInfo.username?.[0]?.toUpperCase() }}
              </el-avatar>
              <div class="upload-overlay">
                <CameraFilled class="upload-icon" />
              </div>
            </div>
          </el-upload>
          <span class="upload-hint">点击更换</span>
        </div>

        <!-- 右侧：用户名和注册时间 -->
        <div class="user-info-wrapper">
          <div class="user-info-item">
            <label>用户名</label>
            <div class="info-value">{{ userInfo.username }}</div>
          </div>
          <div class="user-info-item">
            <label>注册时间</label>
            <div class="info-value">{{ userInfo.createTime }}</div>
          </div>
        </div>
      </div>

      <!-- 第二部分：联系方式和密码 -->
      <div class="profile-section section-contact">
        <div class="form-item">
          <label>邮箱</label>
          <div v-if="editingField === 'email'" class="edit-mode">
            <el-input
              v-model="userInfoForm.email"
              placeholder="请输入邮箱"
              class="apple-input"
            />
            <div class="edit-actions">
              <el-button class="apple-btn apple-btn-secondary" @click="cancelEdit">
                取消
              </el-button>
              <el-button
                class="apple-btn apple-btn-primary"
                type="primary"
                @click="saveUserInfo"
              >
                保存
              </el-button>
            </div>
          </div>
          <div v-else class="readonly-field clickable" @click="editField('email')">
            {{ userInfo.email || '未设置' }}
            <el-icon class="edit-icon"><Edit /></el-icon>
          </div>
        </div>

        <div class="form-item">
          <label>手机号</label>
          <div v-if="editingField === 'phone'" class="edit-mode">
            <el-input
              v-model="userInfoForm.phone"
              placeholder="请输入手机号"
              class="apple-input"
            />
            <div class="edit-actions">
              <el-button class="apple-btn apple-btn-secondary" @click="cancelEdit">
                取消
              </el-button>
              <el-button
                class="apple-btn apple-btn-primary"
                type="primary"
                @click="saveUserInfo"
              >
                保存
              </el-button>
            </div>
          </div>
          <div v-else class="readonly-field clickable" @click="editField('phone')">
            {{ userInfo.phone || '未设置' }}
            <el-icon class="edit-icon"><Edit /></el-icon>
          </div>
        </div>

        <div class="form-item">
          <label>密码</label>
          <div class="readonly-field clickable" @click="handleChangePassword">
            ••••••••
            <el-icon class="edit-icon"><Lock /></el-icon>
          </div>
        </div>
      </div>

      <!-- Slot for additional content -->
      <slot></slot>
    </div>
  </el-dialog>
</template>

<style scoped>
/* Dialog Customization */
:deep(.apple-dialog) {
  background: transparent;
  padding: 0;
  border: none;
  overflow: visible;
}

:deep(.apple-dialog .el-dialog__header) {
  display: none;
}

:deep(.apple-dialog .el-dialog__body) {
  padding: 0;
  background: var(--el-bg-color);
  border-radius: 32px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border: 1px solid var(--el-border-color-light);
}

:deep(.apple-dialog .el-dialog__footer) {
  display: none;
}

/* Dialog Header */
.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28px 32px 24px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.dialog-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  letter-spacing: -0.02em;
}

.close-icon {
  font-size: 18px;
  color: white;
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #ff3b30;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(255, 59, 48, 0.3);
}

.close-icon:hover {
  background: #ff453a;
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.4);
}

/* Profile Edit Content */
.profile-edit-content {
  display: flex;
  flex-direction: column;
  padding: 0;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

/* Profile Sections */
.profile-section {
  padding: 28px 32px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.profile-section:last-child {
  border-bottom: none;
}

/* Section 1: Basic Info */
.section-basic {
  display: flex;
  gap: 28px;
  align-items: center;
}

/* Avatar Wrapper */
.avatar-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.avatar-upload-trigger {
  position: relative;
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.avatar-upload-trigger:hover {
  transform: scale(1.05);
}

.avatar-upload-trigger:hover .upload-overlay {
  opacity: 1;
}

:deep(.user-avatar) {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transition: box-shadow 0.3s ease;
}

.avatar-upload-trigger:hover :deep(.user-avatar) {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.upload-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.upload-icon {
  font-size: 24px;
  color: white;
}

.upload-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

/* User Info Wrapper */
.user-info-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.user-info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-info-item label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  font-weight: 600;
  letter-spacing: -0.01em;
}

.info-value {
  font-size: 16px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

/* Section 2: Contact Info */
.section-contact {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form-item label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  font-weight: 600;
  letter-spacing: -0.01em;
}

/* Readonly Field */
.readonly-field {
  padding: 14px 18px;
  background: var(--el-fill-color-light);
  border-radius: 16px;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
}

.readonly-field.clickable {
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.readonly-field.clickable:hover {
  background: var(--el-bg-color);
  border-color: var(--el-color-primary-light-7);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.edit-icon {
  font-size: 16px;
  color: var(--el-text-color-placeholder);
  transition: color 0.2s ease;
}

.readonly-field.clickable:hover .edit-icon {
  color: var(--el-color-primary);
}

/* Edit Mode */
.edit-mode {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.edit-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

/* Apple Input Style */
:deep(.apple-input) {
  --el-input-border-color: transparent;
  --el-input-bg-color: var(--el-fill-color-light);
  --el-input-hover-border-color: transparent;
  --el-input-focus-border-color: var(--el-color-primary);
  --el-input-border-radius: 14px;
}

:deep(.apple-input .el-input__wrapper) {
  padding: 14px 18px;
  box-shadow: none;
  border-radius: 16px;
  background: var(--el-fill-color-light);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
}

:deep(.apple-input .el-input__wrapper:hover) {
  background: var(--el-bg-color);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

:deep(.apple-input .el-input__wrapper.is-focus) {
  background: var(--el-bg-color);
  box-shadow: 0 0 0 3px var(--el-color-primary-light-9);
}

:deep(.apple-input .el-input__inner) {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

/* Apple Button Style */
.apple-btn {
  padding: 10px 24px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: none;
  letter-spacing: -0.01em;
}

.apple-btn-secondary {
  background: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.apple-btn-secondary:hover {
  background: var(--el-fill-color);
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
}

.apple-btn-primary {
  background: var(--el-color-primary);
  color: white;
  box-shadow: 0 4px 12px var(--el-color-primary-light-3);
}

.apple-btn-primary:hover {
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 6px 16px var(--el-color-primary-light-4);
}

/* Custom Scrollbar for Dialog Content */
.profile-edit-content::-webkit-scrollbar {
  width: 6px;
}

.profile-edit-content::-webkit-scrollbar-track {
  background: transparent;
}

.profile-edit-content::-webkit-scrollbar-thumb {
  background: var(--el-border-color-darker);
  border-radius: 3px;
  transition: background 0.2s ease;
}

.profile-edit-content::-webkit-scrollbar-thumb:hover {
  background: var(--el-text-color-placeholder);
}

/* Animations */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

:deep(.apple-dialog) {
  animation: fadeIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* Responsive */
@media (max-width: 768px) {
  :deep(.apple-dialog .el-dialog__body) {
    border-radius: 24px;
  }

  .profile-section {
    padding: 24px 20px;
  }

  .section-basic {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
  }

  .avatar-wrapper {
    align-self: center;
  }

  .user-info-wrapper {
    gap: 16px;
  }

  .dialog-header {
    padding: 24px 20px;
  }

  .readonly-field {
    padding: 12px 16px;
  }

  :deep(.apple-input .el-input__wrapper) {
    padding: 12px 16px;
  }
}
</style>
