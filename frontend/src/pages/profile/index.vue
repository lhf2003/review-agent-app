<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api/http'
import { useAuthStore } from '../../stores/auth'
import { useThemeStore } from '../../stores/theme'
import CustomScroll from '../../components/CustomScroll.vue'
import HeroSection from './components/HeroSection.vue'
import StatisticsGrid from './components/StatisticsGrid.vue'
import RecentActivity from './components/RecentActivity.vue'
import ProfileEditDialog from './components/ProfileEditDialog.vue'
import PasswordDialog from './components/PasswordDialog.vue'
// import ProfileNav from './components/ProfileNav.vue'
import AchievementsSection from './components/AchievementsSection.vue'
import LearningPathRecommender from '../../components/quiz/LearningPathRecommender.vue'
import PendingReviewReminder from './components/PendingReviewReminder.vue'
import LearningDashboard from './components/LearningDashboard.vue'
import { useUserInfo } from './composables/useUserInfo'
import { useStats } from './composables/useStats'
import { useAnimations } from './composables/useAnimations'
import { useAchievements } from './composables/useAchievements'

const auth = useAuthStore()
const themeStore = useThemeStore()

// Navigation state
const activeSection = ref('overview')

// User Info
const {
  userInfo,
  userInfoForm,
  editingField,
  loadUserInfo,
  editField,
  cancelEdit,
  saveUserInfo,
  handleAvatarUpload,
  beforeAvatarUpload
} = useUserInfo()

// Stats
const {
  stats,
  animatedStats,
  loadStats,
  statCards,
  quickActions
} = useStats()

// Animations
const {
  showContent,
  cardsVisible,
  achievementsVisible,
  initAnimations
  } = useAnimations()

// Achievements
const {
  achievementsData,
  loading,
  loadAchievementsData
} = useAchievements()

// Dialogs
const profileDialog = ref(false)
const passwordDialog = ref(false)
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirm: ''
})

onMounted(() => {
  loadUserInfo()
  loadStats()
  loadAchievementsData()
  initAnimations()
})

async function changePassword() {
  if (!passwordForm.value.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (!passwordForm.value.newPassword || passwordForm.value.newPassword.length < 6) {
    ElMessage.warning('新密码长度至少6位')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirm) {
    ElMessage.warning('两次密码输入不一致')
    return
  }

  try {
    await api.updateUserPassword(
      passwordForm.value.oldPassword,
      passwordForm.value.newPassword
    )
    ElMessage.success('密码修改成功')
    passwordDialog.value = false
    passwordForm.value = { oldPassword: '', newPassword: '', confirm: '' }
  } catch (e) {
    ElMessage.error('密码修改失败: ' + e.message)
  }
}
</script>

<template>
   <div class="dashboard-container">
    <CustomScroll class="dashboard-scroll">
      <!-- Hero Section - 欢迎区 -->
      <HeroSection
        :user-info="userInfo"
        :animated-stats="animatedStats"
        :show-content="showContent"
        @edit="profileDialog = true"
      />

      <!-- Profile Navigation - 分栏导航 -->
      <el-radio-group v-model="activeSection" class="nav-radio-group">
        <el-radio-button value="overview">数据概览</el-radio-button>
        <el-radio-button value="learning-data">学习数据</el-radio-button>
        <el-radio-button value="achievements">学习成就</el-radio-button>
        <el-radio-button value="learning-path">学习路径</el-radio-button>
      </el-radio-group>

      <!-- 数据概览区块 -->
      <div v-show="activeSection === 'overview'" class="overview-content section-transition">
        <!-- 左侧：统计卡片 -->
        <div class="overview-left">
          <div class="section-title">数据概览</div>
          <StatisticsGrid
            :animated-stats="animatedStats"
            :stat-cards="statCards"
            :cards-visible="cardsVisible"
          />
        </div>

        <!-- 右侧：最近活动 + 待复习提醒 -->
        <div class="overview-right">
          <!-- 待复习提醒 -->
          <PendingReviewReminder :cards-visible="showContent" />

          <div class="section-title">最近活动</div>
          <RecentActivity
            :recent-activities="stats.recentActivities"
            :cards-visible="cardsVisible"
          />
        </div>
      </div>

      <!-- Learning Data Section - 学习数据仪表盘 -->
      <div v-show="activeSection === 'learning-data'" class="section-transition">
        <LearningDashboard />
      </div>

      <!-- Achievements Section - 学习成就 -->
      <AchievementsSection
        v-show="activeSection === 'achievements'"
        class="section-transition"
        :achievements="achievementsData.achievements"
        :loading="loading"
        :cards-visible="achievementsVisible"
      />

      <!-- Learning Path Section - 学习路径推荐 -->
      <div v-show="activeSection === 'learning-path'" class="section-transition">
        <LearningPathRecommender :limit="5" />
      </div>
    </CustomScroll>

    <!-- Profile Edit Dialog - 个人信息编辑对话框 -->
    <ProfileEditDialog
      v-model:visible="profileDialog"
      :user-info="userInfo"
      :user-info-form="userInfoForm"
      :editing-field="editingField"
      @edit-field="editField"
      @cancel-edit="cancelEdit"
      @save-user-info="saveUserInfo"
      @handle-avatar-upload="handleAvatarUpload"
    >
      <!-- 修改密码按钮 -->
      <div class="password-section">
        <el-button type="primary" @click="passwordDialog = true; profileDialog = false">
          修改密码
        </el-button>
      </div>
    </ProfileEditDialog>

    <!-- Change Password Dialog - 修改密码对话框 -->
    <PasswordDialog
      v-model:visible="passwordDialog"
      :password-form="passwordForm"
      @change-password="changePassword"
    />
  </div>
</template>

<style scoped>
/* Container */
.dashboard-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.dashboard-scroll {
  flex: 1;
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}

/* Profile Navigation - 分栏导航 */
.nav-radio-group {
  margin-top: -16px;
  margin-bottom: 10px;
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border-radius: 14px;
  box-shadow:
    0 4px 20px rgba(0, 0, 0, 0.06),
    0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  display: flex;
  justify-content: center;
  gap: 6px;
  max-width: 480px;
  margin-left: auto;
  margin-right: auto;
}

.nav-radio-group :deep(.el-radio-button) {
  margin: 0;
  flex: 1;
}

.nav-radio-group :deep(.el-radio-button__inner) {
  width: 100%;
  padding: 10px 16px;
  border-radius: 10px;
  border: none;
  background: transparent;
  color: var(--el-text-color-secondary);
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-radio-group :deep(.el-radio-button__inner:hover) {
  background: rgba(255, 255, 255, 0.6);
  color: var(--el-text-color-primary);
  transform: translateY(-1px);
}

.nav-radio-group :deep(.el-radio-button.is-active .el-radio-button__inner) {
  background: var(--el-color-primary);
  color: white;
  box-shadow:
    0 2px 8px rgba(64, 158, 255, 0.25),
    0 0 0 1px rgba(255, 255, 255, 0.2) inset;
  transform: translateY(-1px);
}

.nav-radio-group :deep(.el-radio-button:first-child .el-radio-button__inner),
.nav-radio-group :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 10px;
}

/* Dark Mode */
html.dark .nav-radio-group {
  background: rgba(40, 40, 40, 0.6);
  box-shadow:
    0 4px 20px rgba(0, 0, 0, 0.3),
    0 0 0 1px rgba(255, 255, 255, 0.08) inset;
}

html.dark .nav-radio-group :deep(.el-radio-button__inner) {
  color: var(--el-text-color-secondary);
}

html.dark .nav-radio-group :deep(.el-radio-button__inner:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: var(--el-text-color-primary);
}

html.dark .nav-radio-group :deep(.el-radio-button.is-active .el-radio-button__inner) {
  background: var(--el-color-primary);
  box-shadow:
    0 2px 8px rgba(64, 158, 255, 0.35),
    0 0 0 1px rgba(255, 255, 255, 0.15) inset;
}

/* Section Titles */
.section-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 20px;
  opacity: 0;
  animation: fadeSlideUp 0.6s ease forwards;
}

.section-title:nth-of-type(2) {
  animation-delay: 0.2s;
}

.section-title:nth-of-type(3) {
  animation-delay: 0.4s;
}

.section-title:nth-of-type(4) {
  animation-delay: 0.6s;
}

/* Overview Content - 两列布局 */
.overview-content {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 24px;
  margin-top: 32px;
}

.overview-left {
  min-width: 0;
}

.overview-right {
  min-width: 0;
}

@keyframes fadeSlideUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Section Transition - 平滑过渡 */
.section-transition {
  transition: opacity 0.3s ease, transform 0.3s ease;
  opacity: 1;
  transform: translateY(0);
}

.section-transition[style*="display: none"] {
  opacity: 0;
  transform: translateY(-8px);
}

.password-section {
  display: flex;
  justify-content: center;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}

/* Responsive Design */
@media (max-width: 1024px) {
  .overview-content {
    grid-template-columns: 1fr;
  }

  .nav-radio-group {
    max-width: 100%;
  }
}

@media (max-width: 768px) {
  .dashboard-scroll {
    padding: 16px;
  }

  .nav-radio-group {
    margin-top: -12px;
    margin-bottom: 20px;
    padding: 5px;
    border-radius: 12px;
  }

  .nav-radio-group :deep(.el-radio-button__inner) {
    padding: 8px 12px;
    font-size: 13px;
  }

  .section-title {
    font-size: 18px;
  }

  .overview-content {
    gap: 20px;
  }
}

/* Large screens */
@media (min-width: 1600px) {
  .dashboard-scroll {
    max-width: 1600px;
  }

  .overview-content {
    grid-template-columns: 1fr 480px;
  }

  .nav-radio-group {
    max-width: 520px;
  }
}
</style>
