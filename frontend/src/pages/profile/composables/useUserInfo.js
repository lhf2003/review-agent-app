import { ref } from 'vue'
import { api } from '../../../api/http'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../../stores/auth'

export function useUserInfo() {
  const auth = useAuthStore()

  const userInfo = ref({
    username: '',
    email: '',
    phone: '',
    avatar: '',
    createTime: ''
  })

  const userInfoForm = ref({
    email: '',
    phone: ''
  })

  const editingField = ref(null)

  async function loadUserInfo() {
    try {
      const resp = await api.getUserInfo()
      const data = resp?.data || resp
      userInfo.value = {
        username: data.username || '',
        email: data.email || '',
        phone: data.phone || '',
        avatar: data.avatar || '',
        createTime: formatDateTime(data.createTime)
      }
      userInfoForm.value = {
        email: data.email || '',
        phone: data.phone || ''
      }
    } catch (e) {
      ElMessage.error('加载用户信息失败')
    }
  }

  function editField(field) {
    editingField.value = field
  }

  function cancelEdit() {
    editingField.value = null
    userInfoForm.value = {
      email: userInfo.value.email,
      phone: userInfo.value.phone
    }
  }

  async function saveUserInfo() {
    try {
      await api.updateUserInfo({
        email: userInfoForm.value.email,
        phone: userInfoForm.value.phone
      })
      ElMessage.success('保存成功')
      userInfo.value.email = userInfoForm.value.email
      userInfo.value.phone = userInfoForm.value.phone
      editingField.value = null
    } catch (e) {
      ElMessage.error('保存失败: ' + e.message)
    }
  }

  async function handleAvatarUpload(file) {
    const formData = new FormData()
    formData.append('avatar', file.raw)

    try {
      await api.uploadAvatar(formData)
      ElMessage.success('头像更新成功')
      loadUserInfo()
      if (auth.username) {
        auth.avatar = userInfo.value.avatar
      }
    } catch (e) {
      ElMessage.error('头像上传失败')
    }
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

  function formatDateTime(dateStr) {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    })
  }

  return {
    userInfo,
    userInfoForm,
    editingField,
    loadUserInfo,
    editField,
    cancelEdit,
    saveUserInfo,
    handleAvatarUpload,
    beforeAvatarUpload
  }
}
