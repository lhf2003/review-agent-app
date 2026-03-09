<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { api } from '../../api/http'
import { useAuthStore } from '../../stores/auth'
import CustomScroll from '../../components/CustomScroll.vue'
import AchievementsSection from './components/AchievementsSection.vue'
import LearningHeatmapChart from '../../components/charts/LearningHeatmapChart.vue'

const router = useRouter()
const auth = useAuthStore()

function goBack() {
  router.back()
}

// User Info
const userInfo = ref({
  name: '',
  title: '',
  avatar: ''
})

// Loading states
const loading = ref({
  achievements: false,
  heatmap: false
})

// Achievements data
const achievementsData = ref([])

// Heatmap data
const heatmapData = ref({})

// Load user info
async function loadUserInfo() {
  try {
    const data = await api.getUserInfo()
    if (data) {
      userInfo.value = {
        name: data.nickname || data.username || 'User',
        title: data.title || 'Developer',
        avatar: data.avatar || ''
      }
    }
  } catch (error) {
    console.error('Failed to load user info:', error)
  }
}

// Load achievements
async function loadAchievements() {
  loading.value.achievements = true
  try {
    const resp = await api.getUserStats()
    const data = resp?.data || resp || {}
    achievementsData.value = data.achievements || []
  } catch (error) {
    console.error('Failed to load achievements:', error)
    achievementsData.value = []
  } finally {
    loading.value.achievements = false
  }
}

// Load heatmap data
async function loadHeatmapData() {
  loading.value.heatmap = true
  try {
    // Try to get dashboard data which may contain heatmap info
    const dashboardData = await api.getDashboard()
    const heatmap = dashboardData?.heatmap || dashboardData?.activityHeatmap || []

    // Convert array to object format expected by heatmap component
    const dataObj = {}
    if (Array.isArray(heatmap)) {
      heatmap.forEach(item => {
        const date = item.day || item.date
        const value = item.value || item.count || item.intensity || 0
        if (date) {
          dataObj[date] = value
        }
      })
    }

    // If no data from API, generate mock data for demonstration
    if (Object.keys(dataObj).length === 0) {
      generateMockHeatmapData(dataObj)
    }

    heatmapData.value = dataObj
  } catch (error) {
    console.error('Failed to load heatmap data:', error)
    // Generate mock data on error
    const mockData = {}
    generateMockHeatmapData(mockData)
    heatmapData.value = mockData
  } finally {
    loading.value.heatmap = false
  }
}

// Generate mock heatmap data for demonstration (past 6 months)
function generateMockHeatmapData(dataObj) {
  const now = new Date()
  for (let i = 0; i < 180; i++) { // ~6 months
    const date = new Date(now)
    date.setDate(date.getDate() - i)
    const dateStr = date.toISOString().split('T')[0]
    // Random activity level 0-5
    const value = Math.random() > 0.6 ? Math.floor(Math.random() * 5) + 1 : 0
    if (value > 0) {
      dataObj[dateStr] = value
    }
  }
}

// Dialog states
const profileDialog = ref(false)
const passwordDialog = ref(false)
const userInfoForm = ref({
  name: '',
  title: ''
})
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirm: ''
})

function openProfileDialog() {
  userInfoForm.value = {
    name: userInfo.value.name,
    title: userInfo.value.title
  }
  profileDialog.value = true
}

async function saveUserInfo() {
  try {
    await api.updateUserInfo(userInfoForm.value)
    userInfo.value.name = userInfoForm.value.name
    userInfo.value.title = userInfoForm.value.title
    ElMessage.success('Profile updated successfully')
    profileDialog.value = false
  } catch (error) {
    ElMessage.error('Failed to update profile: ' + error.message)
  }
}

function openPasswordDialog() {
  passwordDialog.value = true
}

async function changePassword() {
  if (!passwordForm.value.oldPassword) {
    ElMessage.warning('Please enter current password')
    return
  }
  if (!passwordForm.value.newPassword || passwordForm.value.newPassword.length < 6) {
    ElMessage.warning('New password must be at least 6 characters')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirm) {
    ElMessage.warning('Passwords do not match')
    return
  }

  try {
    await api.updateUserPassword(
      passwordForm.value.oldPassword,
      passwordForm.value.newPassword
    )
    ElMessage.success('Password changed successfully')
    passwordDialog.value = false
    passwordForm.value = { oldPassword: '', newPassword: '', confirm: '' }
  } catch (error) {
    ElMessage.error('Failed to change password: ' + error.message)
  }
}

onMounted(() => {
  loadUserInfo()
  loadAchievements()
  loadHeatmapData()
})
</script>

<template>
  <div class="profile-page">
    <CustomScroll class="profile-scroll">
      <div class="profile-header">
        <button class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </button>
      </div>
      <div class="profile-grid">
        <!-- Left Column: User Info -->
        <div class="col-left">
          <section class="card glass profile-card">
            <div class="profile-hero">
              <div class="avatar-lg">
                <img
                  v-if="userInfo.avatar"
                  :src="userInfo.avatar"
                  alt="Avatar"
                  @error="$event.target.style.display = 'none'"
                />
                <span v-else class="avatar-fallback">{{ userInfo.name?.charAt(0) || 'U' }}</span>
              </div>
              <h1 class="user-name">{{ userInfo.name }}</h1>
              <p class="user-title">{{ userInfo.title }}</p>
            </div>
          </section>

          <!-- Heatmap Card -->
          <section class="card glass heatmap-card">
            <div class="card-header">
              <span class="card-title">Activity</span>
            </div>
            <div class="heatmap-container">
              <LearningHeatmapChart
                v-if="!loading.heatmap"
                :data="heatmapData"
                :loading="loading.heatmap"
                theme="nebula"
              />
              <div v-else class="heatmap-loading">
                <el-skeleton :rows="3" animated />
              </div>
            </div>
          </section>
        </div>

        <!-- Right Column: Achievements & Other Content -->
        <div class="col-right">
          <!-- Achievements Section -->
          <section class="card glass achievements-card">
            <AchievementsSection
              :achievements="achievementsData"
              :loading="loading"
              :cards-visible="true"
            />
          </section>
        </div>
      </div>
    </CustomScroll>

    <!-- Edit Profile Dialog -->
    <el-dialog v-model="profileDialog" title="Edit Profile" width="400px">
      <el-form label-position="top">
        <el-form-item label="Name">
          <el-input v-model="userInfoForm.name" />
        </el-form-item>
        <el-form-item label="Title">
          <el-input v-model="userInfoForm.title" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialog = false">Cancel</el-button>
        <el-button type="primary" @click="saveUserInfo">Save</el-button>
      </template>
    </el-dialog>

    <!-- Change Password Dialog -->
    <el-dialog v-model="passwordDialog" title="Change Password" width="400px">
      <el-form label-position="top">
        <el-form-item label="Current Password">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="New Password">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="Confirm Password">
          <el-input v-model="passwordForm.confirm" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialog = false">Cancel</el-button>
        <el-button type="primary" @click="changePassword">Change</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
// Glass Effect
.glass {
  background: var(--glass-surface);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--glass-border);
  border-top: 1px solid var(--glass-highlight);
}

// Container
.profile-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.profile-scroll {
  flex: 1;
  padding: 16px 32px 24px;
}

// Back Button Header
.profile-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  max-width: 1600px;
  margin-left: auto;
  margin-right: auto;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 100px;
  font-size: 13px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.8);
  background: rgba(255, 248, 245, 0.05);
  border: 1px solid rgba(255, 248, 245, 0.1);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 248, 245, 0.1);
    color: rgba(255, 255, 255, 0.95);
  }
}

// Grid Layout - Two Columns
.profile-grid {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 24px;
  max-width: 1600px;
  margin: 0 auto;
}

// Columns
.col-left {
  display: flex;
  flex-direction: column;
}

.col-right {
  display: flex;
  flex-direction: column;
}

// Card Styles
.card {
  border-radius: 24px;
  padding: 24px;
  display: flex;
  flex-direction: column;
}

// Profile Card
.profile-card {
  min-height: 320px;
}

.profile-hero {
  text-align: center;
  padding: 16px 0;
}

.avatar-lg {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: var(--gradient-warm);
  border: 4px solid rgba(255, 255, 255, 0.1);
  margin: 0 auto 20px;
  box-shadow: 0 0 40px var(--accent-glow-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.avatar-lg img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  font-size: 48px;
  font-weight: 600;
  color: white;
}

.user-name {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.user-title {
  color: var(--text-secondary);
  font-size: 14px;
  margin-bottom: 24px;
}

// Card Header (for heatmap)
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

// Heatmap Card
.heatmap-card {
  margin-top: 24px;
  padding: 20px;
}

.heatmap-container {
  width: 100%;
  min-height: 280px;
}

.heatmap-loading {
  width: 100%;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

// Heatmap Theme Overrides - Nebula warm colors
.heatmap-card {
  :deep(.learning-heatmap-chart) {
    .month-grid {
      .day-block {
        border-color: var(--glass-border);
      }
    }

    .nav-btn {
      background: rgba(255, 255, 255, 0.05);
      border-color: rgba(255, 255, 255, 0.1);

      &:hover:not(:disabled) {
        background: rgba(255, 255, 255, 0.1);
        border-color: rgba(255, 255, 255, 0.2);
      }
    }
  }
}

// Achievements Card - Override AchievementsSection styles for nebula theme
.achievements-card {
  :deep(.achievements-section) {
    padding: 0;
    background: transparent;
    border: none;
    box-shadow: none;
    backdrop-filter: none;

    &.is-dark {
      background: transparent;
      border: none;
      box-shadow: none;
    }
  }

  :deep(.achievements-header) {
    border-bottom-color: rgba(255, 255, 255, 0.08);
    margin-bottom: 20px;
    padding-bottom: 16px;

    h3 {
      color: var(--text-primary);
    }
  }

  :deep(.header-icon) {
    background: var(--gradient-warm);
    box-shadow: 0 4px 12px var(--accent-glow-soft);
  }

  :deep(.summary-text) {
    color: var(--text-secondary);
  }

  :deep(.filter-control) {
    background: rgba(255, 255, 255, 0.05);
  }

  :deep(.filter-button) {
    color: var(--text-secondary);

    &:hover:not(.active) {
      background: rgba(255, 255, 255, 0.08);
    }

    &.active {
      background: rgba(255, 255, 255, 0.1);
      color: var(--text-primary);
    }
  }

  :deep(.filter-count) {
    background: rgba(255, 255, 255, 0.1);

    .filter-button.active & {
      background: rgba(204, 102, 51, 0.3);
      color: var(--accent-tertiary);
    }
  }

  :deep(.achievement-card) {
    background: rgba(255, 248, 245, 0.03);
    border: 1px solid var(--glass-border);
    box-shadow: none;

    &:hover {
      background: rgba(255, 248, 245, 0.06);
      border-color: rgba(255, 255, 255, 0.1);
      transform: translateY(-3px);
    }

    &.locked {
      opacity: 0.6;
    }
  }

  :deep(.icon-circle) {
    background: var(--gradient-warm);
    box-shadow: 0 4px 12px var(--accent-glow-soft);

    &.is-locked {
      background: rgba(255, 255, 255, 0.1);
    }
  }

  :deep(.achievement-name) {
    color: var(--text-primary);
  }

  :deep(.achievement-desc) {
    color: var(--text-secondary);
  }

  :deep(.unlock-date) {
    background: rgba(204, 102, 51, 0.2);
    color: var(--accent-tertiary);
  }

  :deep(.progress-track) {
    background: rgba(255, 255, 255, 0.1);
  }

  :deep(.progress-fill) {
    background: var(--gradient-warm);
  }

  :deep(.progress-label) {
    color: var(--text-secondary);
  }

  :deep(.progress-track-ring) {
    stroke: rgba(255, 255, 255, 0.1);
  }

  :deep(.progress-fill-ring) {
    stroke: var(--accent-tertiary);
  }

  :deep(.progress-text) {
    color: var(--accent-tertiary);
  }

  :deep(.skeleton-card) {
    background: rgba(255, 248, 245, 0.03);
    border: 1px solid var(--glass-border);
  }
}

// Responsive
@media (max-width: 1200px) {
  .profile-grid {
    grid-template-columns: 300px 1fr;
    gap: 20px;
  }
}

@media (max-width: 900px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .col-left {
    max-width: 400px;
    margin: 0 auto;
    width: 100%;
  }
}

@media (max-width: 768px) {
  .profile-scroll {
    padding: 16px;
  }

  .card {
    padding: 20px;
  }

  .avatar-lg {
    width: 100px;
    height: 100px;
  }

  .user-name {
    font-size: 20px;
  }
}
</style>
