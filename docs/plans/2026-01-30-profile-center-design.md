# 个人中心 (Profile Page) 实现计划

## 概述

**目标**: 实现个人中心页面，提供个人信息管理、数据概览和快捷入口功能。

**优先级**: 核心功能版本

**设计风格**: Apple macOS 设置风格 + Modern Glass 毛玻璃效果

---

## 一、后端实现

### 1.1 新增用户统计接口

#### 需求
提供一个综合统计接口，返回用户的关键数据和最近活动记录。

#### 接口定义
```
GET /user/stats
Header: userId: Long

Response:
{
  "code": 200,
  "data": {
    "syncFileCount": 100,
    "analyzedCount": 85,
    "collectionCount": 12,
    "tagCount": 45,
    "quizCompletedCount": 23,
    "learningDays": 30,
    "recentActivities": [
      {
        "type": "sync",
        "time": "2026-01-30 10:00:00",
        "detail": "同步了5个文件"
      },
      {
        "type": "collection",
        "time": "2026-01-29 15:30:00",
        "detail": "创建了合集\"Java并发问题集\""
      },
      ...
    ]
  }
}
```

#### 实现步骤

**Step 1: 创建统计 VO 类**

文件: `backend/src/main/java/com/review/agent/entity/vo/UserStatsVo.java`

```java
package com.review.agent.entity.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserStatsVo {
    private Long syncFileCount;
    private Long analyzedCount;
    private Long collectionCount;
    private Long tagCount;
    private Long quizCompletedCount;
    private Long learningDays;
    private List<RecentActivityVo> recentActivities;

    @Data
    public static class RecentActivityVo {
        private String type;  // sync, collection, quiz, report
        private String time;
        private String detail;
    }
}
```

**Step 2: 创建统计 Service 方法**

文件: `backend/src/main/java/com/review/agent/service/UserService.java`

在 `UserService` 中添加方法：

```java
/**
 * 获取用户统计数据
 * @param userId 用户ID
 * @return 统计数据
 */
public UserStatsVo getUserStats(Long userId) {
    UserStatsVo stats = new UserStatsVo();

    // 1. 统计已同步文件数
    stats.setSyncFileCount(dataInfoRepository.countByUserId(userId));

    // 2. 统计已分析结果数
    stats.setAnalyzedCount(analysisResultRepository.countByUserId(userId));

    // 3. 统计合集数量
    stats.setCollectionCount(analysisCollectionRepository.countByUserId(userId));

    // 4. 统计标签数量 (主标签 + 子标签)
    long mainTagCount = mainTagRepository.countByUserId(userId);
    long subTagCount = subTagRepository.countByUserId(userId);
    stats.setTagCount(mainTagCount + subTagCount);

    // 5. 统计完成的测验数
    stats.setQuizCompletedCount(quizRecordRepository.countByUserIdAndStatus(userId, 1));

    // 6. 计算学习天数
    UserInfo userInfo = findById(userId);
    if (userInfo != null && userInfo.getCreateTime() != null) {
        long daysBetween = ChronoUnit.DAYS.between(
            userInfo.getCreateTime().toInstant(),
            Instant.now()
        );
        stats.setLearningDays(Math.max(1, daysBetween));
    }

    // 7. 获取最近活动 (最近10条)
    stats.setRecentActivities(getRecentActivities(userId));

    return stats;
}

/**
 * 获取用户最近活动记录
 */
private List<UserStatsVo.RecentActivityVo> getRecentActivities(Long userId) {
    List<UserStatsVo.RecentActivityVo> activities = new ArrayList<>();

    // 1. 最近同步记录 (取前3条)
    List<SyncRecord> syncRecords = syncRecordRepository
        .findTop3ByUserIdOrderByCreateTimeDesc(userId);
    for (SyncRecord record : syncRecords) {
        UserStatsVo.RecentActivityVo activity = new UserStatsVo.RecentActivityVo();
        activity.setType("sync");
        activity.setTime(formatTime(record.getCreateTime()));
        activity.setDetail("同步了" + record.getSyncCount() + "个文件，耗时" + record.getSpendTime() + "秒");
        activities.add(activity);
    }

    // 2. 最近创建的合集 (取前3条)
    List<AnalysisCollection> collections = analysisCollectionRepository
        .findTop3ByUserIdOrderByCreatedTimeDesc(userId);
    for (AnalysisCollection collection : collections) {
        UserStatsVo.RecentActivityVo activity = new UserStatsVo.RecentActivityVo();
        activity.setType("collection");
        activity.setTime(formatTime(collection.getCreatedTime()));
        activity.setDetail("创建了合集\"" + collection.getName() + "\"");
        activities.add(activity);
    }

    // 3. 最近完成的测验 (取前2条)
    List<QuizRecord> quizzes = quizRecordRepository
        .findTop2ByUserIdAndStatusOrderByCreatedTimeDesc(userId, 1);
    for (QuizRecord quiz : quizzes) {
        UserStatsVo.RecentActivityVo activity = new UserStatsVo.RecentActivityVo();
        activity.setType("quiz");
        activity.setTime(formatTime(quiz.getCreatedTime()));
        activity.setDetail("完成测验，总分" + quiz.getTotalScore());
        activities.add(activity);
    }

    // 4. 最近生成的报告 (取前2条)
    List<ReportData> reports = reportDataRepository
        .findTop2ByUserIdOrderByCreateTimeDesc(userId);
    for (ReportData report : reports) {
        UserStatsVo.RecentActivityVo activity = new UserStatsVo.RecentActivityVo();
        activity.setType("report");
        activity.setTime(formatTime(report.getCreateTime()));
        String reportType = report.getType() == 1 ? "日报" : "周报";
        activity.setDetail("生成" + reportType);
        activities.add(activity);
    }

    // 5. 按时间排序，只保留前10条
    activities.sort((a, b) -> b.getTime().compareTo(a.getTime()));
    return activities.stream().limit(10).toList();
}

private String formatTime(Date date) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    return sdf.format(date);
}
```

**Step 3: 添加 Repository 方法**

需要在各 Repository 中添加以下方法：

```java
// DataInfoRepository
Long countByUserId(Long userId);

// AnalysisResultRepository
Long countByUserId(Long userId);

// AnalysisCollectionRepository
Long countByUserId(Long userId);
List<AnalysisCollection> findTop3ByUserIdOrderByCreatedTimeDesc(Long userId);

// MainTagRepository
Long countByUserId(Long userId);

// SubTagRepository
Long countByUserId(Long userId);

// QuizRecordRepository
Long countByUserIdAndStatus(Long userId, Integer status);
List<QuizRecord> findTop2ByUserIdAndStatusOrderByCreatedTimeDesc(Long userId, Integer status);

// SyncRecordRepository
List<SyncRecord> findTop3ByUserIdOrderByCreateTimeDesc(Long userId);

// ReportDataRepository
List<ReportData> findTop2ByUserIdOrderByCreateTimeDesc(Long userId);
```

**Step 4: 添加 Controller 接口**

文件: `backend/src/main/java/com/review/agent/controller/UserController.java`

```java
/**
 * 获取用户统计数据
 */
@GetMapping("/stats")
public BaseResponse<UserStatsVo> getUserStats(@RequestHeader("userId") Long userId) {
    UserInfo userInfo = userService.findById(userId);
    if (userInfo == null) {
        return ResultUtil.error("user not found");
    }

    return ResultUtil.success(userService.getUserStats(userId));
}
```

**Step 5: 前端 API 封装**

文件: `frontend/src/api/http.ts`

```typescript
// 获取用户统计
export const getUserStats = () => {
  return request.get('/user/stats')
}
```

---

### 1.2 复用现有接口

以下接口已存在，无需修改：
- `GET /user/info` - 获取用户信息
- `POST /user/info/update` - 更新用户信息
- `POST /user/info/upload/avatar` - 上传头像
- `POST /user/info/update/password` - 修改密码

---

## 二、前端实现

### 2.1 创建 ProfilePage 组件

#### 文件路径
`frontend/src/pages/ProfilePage.vue`

#### 组件结构

```vue
<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User, DocumentCopy, Notebook, Edit, PriceTag,
  TrendCharts, Clock, Setting, FolderOpened
} from '@element-plus/icons-vue'
import { api } from '../api/http'
import CustomScroll from '../components/CustomScroll.vue'

const router = useRouter()
const activeSection = ref('info')

// 用户信息
const userInfo = ref({
  username: '',
  email: '',
  phone: '',
  avatar: '',
  createTime: ''
})

const editingField = ref(null)
const userInfoForm = ref({
  email: '',
  phone: ''
})

// 统计数据
const stats = ref({
  syncFileCount: 0,
  analyzedCount: 0,
  collectionCount: 0,
  tagCount: 0,
  quizCompletedCount: 0,
  learningDays: 0,
  recentActivities: []
})

// 密码修改
const passwordDialog = ref(false)
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirm: ''
})

// 头像上传
const uploadRef = ref(null)

onMounted(() => {
  loadUserInfo()
  loadStats()
})

async function loadUserInfo() {
  try {
    const resp = await api.getUserInfo()
    const data = resp?.data || resp
    userInfo.value = {
      username: data.username || '',
      email: data.email || '',
      phone: data.phone || '',
      avatar: data.avatar || '',
      createTime: data.createTime || ''
    }
    userInfoForm.value = {
      email: data.email || '',
      phone: data.phone || ''
    }
  } catch (e) {
    ElMessage.error('加载用户信息失败')
  }
}

async function loadStats() {
  try {
    const resp = await api.getUserStats()
    stats.value = resp?.data || {}
  } catch (e) {
    ElMessage.error('加载统计数据失败')
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
    await api.updatePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    ElMessage.success('密码修改成功')
    passwordDialog.value = false
    passwordForm.value = { oldPassword: '', newPassword: '', confirm: '' }
  } catch (e) {
    ElMessage.error('密码修改失败: ' + e.message)
  }
}

function navigateTo(path) {
  router.push(path)
}

function getActivityIcon(type) {
  const icons = {
    sync: 'DocumentCopy',
    collection: 'FolderOpened',
    quiz: 'Edit',
    report: 'Notebook'
  }
  return icons[type] || 'Document'
}
</script>

<template>
  <div class="profile-container">
    <!-- 左侧导航栏 -->
    <div class="profile-sidebar">
      <div
        class="nav-item"
        :class="{ active: activeSection === 'info' }"
        @click="activeSection = 'info'"
      >
        <el-icon><User /></el-icon>
        <span>个人信息</span>
      </div>
      <div
        class="nav-item"
        :class="{ active: activeSection === 'stats' }"
        @click="activeSection = 'stats'"
      >
        <el-icon><TrendCharts /></el-icon>
        <span>数据概览</span>
      </div>
      <div
        class="nav-item"
        :class="{ active: activeSection === 'shortcuts' }"
        @click="activeSection = 'shortcuts'"
      >
        <el-icon><PriceTag /></el-icon>
        <span>快捷入口</span>
      </div>
    </div>

    <!-- 右侧内容区 -->
    <CustomScroll class="profile-content">
      <!-- 顶部欢迎区 -->
      <div class="welcome-section">
        <el-avatar :size="120" :src="userInfo.avatar">
          {{ userInfo.username?.[0]?.toUpperCase() }}
        </el-avatar>
        <div class="welcome-text">
          <h2>欢迎回来，{{ userInfo.username }}</h2>
          <p>今天是你学习的第 {{ stats.learningDays }} 天</p>
        </div>
      </div>

      <!-- 个人信息卡片 -->
      <div v-show="activeSection === 'info'" class="info-card">
        <h3>个人信息</h3>

        <!-- 头像上传 -->
        <div class="avatar-section">
          <el-upload
            ref="uploadRef"
            :show-file-list="false"
            :before-upload="beforeAvatarUpload"
            :on-change="handleAvatarUpload"
            :auto-upload="false"
            accept="image/*"
          >
            <div class="avatar-upload">
              <el-avatar :size="80" :src="userInfo.avatar">
                {{ userInfo.username?.[0]?.toUpperCase() }}
              </el-avatar>
              <div class="upload-overlay">
                <el-icon><Edit /></el-icon>
              </div>
            </div>
          </el-upload>
          <span class="upload-hint">点击更换头像</span>
        </div>

        <!-- 基本信息表单 -->
        <div class="info-form">
          <div class="form-item">
            <label>用户名</label>
            <div class="readonly-field">{{ userInfo.username }}</div>
          </div>

          <div class="form-item">
            <label>邮箱</label>
            <div v-if="editingField === 'email'" class="edit-mode">
              <el-input v-model="userInfoForm.email" placeholder="请输入邮箱" />
              <div class="edit-actions">
                <el-button size="small" @click="cancelEdit">取消</el-button>
                <el-button size="small" type="primary" @click="saveUserInfo">保存</el-button>
              </div>
            </div>
            <div v-else class="readonly-field" @click="editField('email')">
              {{ userInfo.email || '未设置' }}
              <el-icon class="edit-icon"><Edit /></el-icon>
            </div>
          </div>

          <div class="form-item">
            <label>手机号</label>
            <div v-if="editingField === 'phone'" class="edit-mode">
              <el-input v-model="userInfoForm.phone" placeholder="请输入手机号" />
              <div class="edit-actions">
                <el-button size="small" @click="cancelEdit">取消</el-button>
                <el-button size="small" type="primary" @click="saveUserInfo">保存</el-button>
              </div>
            </div>
            <div v-else class="readonly-field" @click="editField('phone')">
              {{ userInfo.phone || '未设置' }}
              <el-icon class="edit-icon"><Edit /></el-icon>
            </div>
          </div>

          <div class="form-item">
            <label>注册时间</label>
            <div class="readonly-field">{{ userInfo.createTime }}</div>
          </div>
        </div>

        <!-- 修改密码按钮 -->
        <div class="password-section">
          <el-button type="primary" @click="passwordDialog = true">
            修改密码
          </el-button>
        </div>
      </div>

      <!-- 数据概览卡片 -->
      <div v-show="activeSection === 'stats'" class="stats-card">
        <h3>数据概览</h3>

        <!-- 统计卡片网格 -->
        <div class="stats-grid">
          <div class="stat-item">
            <el-icon class="stat-icon" color="#409EFF"><DocumentCopy /></el-icon>
            <div class="stat-content">
              <div class="stat-number">{{ stats.syncFileCount }}</div>
              <div class="stat-label">已同步文件</div>
            </div>
          </div>

          <div class="stat-item">
            <el-icon class="stat-icon" color="#67C23A"><FolderOpened /></el-icon>
            <div class="stat-content">
              <div class="stat-number">{{ stats.analyzedCount }}</div>
              <div class="stat-label">已分析结果</div>
            </div>
          </div>

          <div class="stat-item">
            <el-icon class="stat-icon" color="#E6A23C"><FolderOpened /></el-icon>
            <div class="stat-content">
              <div class="stat-number">{{ stats.collectionCount }}</div>
              <div class="stat-label">合集数量</div>
            </div>
          </div>

          <div class="stat-item">
            <el-icon class="stat-icon" color="#F56C6C"><PriceTag /></el-icon>
            <div class="stat-content">
              <div class="stat-number">{{ stats.tagCount }}</div>
              <div class="stat-label">标签数量</div>
            </div>
          </div>

          <div class="stat-item">
            <el-icon class="stat-icon" color="#909399"><Edit /></el-icon>
            <div class="stat-content">
              <div class="stat-number">{{ stats.quizCompletedCount }}</div>
              <div class="stat-label">测验完成</div>
            </div>
          </div>

          <div class="stat-item">
            <el-icon class="stat-icon" color="#409EFF"><TrendCharts /></el-icon>
            <div class="stat-content">
              <div class="stat-number">{{ stats.learningDays }}</div>
              <div class="stat-label">学习天数</div>
            </div>
          </div>
        </div>

        <!-- 最近活动时间轴 -->
        <div class="recent-activities">
          <h4>最近活动</h4>
          <el-timeline v-if="stats.recentActivities.length > 0">
            <el-timeline-item
              v-for="(activity, index) in stats.recentActivities"
              :key="index"
              :timestamp="activity.time"
              placement="top"
            >
              <div class="activity-item">
                <el-icon><component :is="getActivityIcon(activity.type)" /></el-icon>
                <span>{{ activity.detail }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无最近活动" />
        </div>
      </div>

      <!-- 快捷入口卡片 -->
      <div v-show="activeSection === 'shortcuts'" class="shortcuts-card">
        <h3>快捷入口</h3>

        <div class="shortcuts-grid">
          <div class="shortcut-item" @click="navigateTo('/collections')">
            <el-icon size="32" color="#409EFF"><FolderOpened /></el-icon>
            <span>我的合集</span>
          </div>

          <div class="shortcut-item" @click="navigateTo('/data')">
            <el-icon size="32" color="#67C23A"><DocumentCopy /></el-icon>
            <span>待分析</span>
          </div>

          <div class="shortcut-item" @click="navigateTo('/report')">
            <el-icon size="32" color="#E6A23C"><Notebook /></el-icon>
            <span>最近报告</span>
          </div>

          <div class="shortcut-item" @click="navigateTo('/collections')">
            <el-icon size="32" color="#F56C6C"><Edit /></el-icon>
            <span>学习测验</span>
          </div>

          <div class="shortcut-item" @click="navigateTo('/tags')">
            <el-icon size="32" color="#909399"><PriceTag /></el-icon>
            <span>标签管理</span>
          </div>

          <div class="shortcut-item" @click="navigateTo('/word-cloud')">
            <el-icon size="32" color="#409EFF"><TrendCharts /></el-icon>
            <span>数据统计</span>
          </div>

          <div class="shortcut-item" @click="navigateTo('/sync')">
            <el-icon size="32" color="#67C23A"><Clock /></el-icon>
            <span>同步历史</span>
          </div>

          <div class="shortcut-item" @click="navigateTo('/config')">
            <el-icon size="32" color="#E6A23C"><Setting /></el-icon>
            <span>系统配置</span>
          </div>
        </div>
      </div>
    </CustomScroll>

    <!-- 修改密码对话框 -->
    <el-dialog
      v-model="passwordDialog"
      title="修改密码"
      width="400px"
      :before-close="() => { passwordDialog = false; passwordForm = { oldPassword: '', newPassword: '', confirm: '' } }"
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
        <el-button @click="passwordDialog = false">取消</el-button>
        <el-button type="primary" @click="changePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-container {
  display: flex;
  height: calc(100vh - 24px);
  gap: 24px;
}

/* 左侧导航栏 */
.profile-sidebar {
  width: 200px;
  padding: 24px 16px;
  background: var(--el-bg-color);
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid var(--el-border-color-light);
  backdrop-filter: blur(10px);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  color: var(--el-text-color-secondary);
}

.nav-item:hover {
  background: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
}

.nav-item.active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.nav-item .el-icon {
  font-size: 20px;
}

/* 右侧内容区 */
.profile-content {
  flex: 1;
  padding-right: 8px;
}

/* 欢迎区 */
.welcome-section {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 32px;
  background: linear-gradient(135deg, var(--el-color-primary-light-9), var(--el-color-primary-light-8));
  border-radius: 16px;
  margin-bottom: 24px;
}

.welcome-text h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.welcome-text p {
  margin: 0;
  color: var(--el-text-color-secondary);
}

/* 卡片通用样式 */
.info-card,
.stats-card,
.shortcuts-card {
  background: var(--el-bg-color);
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid var(--el-border-color-light);
  padding: 32px;
  margin-bottom: 24px;
}

.info-card h3,
.stats-card h3,
.shortcuts-card h3 {
  margin: 0 0 24px 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

/* 个人信息卡片 */
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-bottom: 32px;
}

.avatar-upload {
  position: relative;
  cursor: pointer;
}

.upload-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
  color: white;
}

.avatar-upload:hover .upload-overlay {
  opacity: 1;
}

.upload-hint {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.info-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

.readonly-field {
  padding: 12px 16px;
  background: var(--el-fill-color-light);
  border-radius: 12px;
  color: var(--el-text-color-primary);
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  transition: all 0.3s ease;
}

.readonly-field:hover {
  background: var(--el-fill-color);
}

.readonly-field:empty::after {
  content: '未设置';
  color: var(--el-text-color-placeholder);
}

.edit-icon {
  font-size: 16px;
  color: var(--el-text-color-placeholder);
}

.edit-mode {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edit-actions {
  display: flex;
  gap: 8px;
}

.password-section {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}

/* 数据概览卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--el-fill-color-light);
  border-radius: 12px;
  transition: all 0.3s ease;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  background: var(--el-fill-color);
}

.stat-icon {
  font-size: 32px;
}

.stat-number {
  font-size: 28px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.recent-activities {
  margin-top: 32px;
}

.recent-activities h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.activity-item .el-icon {
  color: var(--el-color-primary);
}

/* 快捷入口卡片 */
.shortcuts-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.shortcut-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 24px;
  background: var(--el-fill-color-light);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.shortcut-item:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  background: var(--el-fill-color);
}

.shortcut-item span {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

/* 深色模式适配 */
html.dark .profile-sidebar,
html.dark .info-card,
html.dark .stats-card,
html.dark .shortcuts-card {
  background: rgba(30, 30, 30, 0.9);
  border-color: rgba(255, 255, 255, 0.1);
}

html.dark .stat-item,
html.dark .shortcut-item {
  background: rgba(255, 255, 255, 0.05);
}

html.dark .stat-item:hover,
html.dark .shortcut-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

html.dark .readonly-field {
  background: rgba(255, 255, 255, 0.05);
}

html.dark .readonly-field:hover {
  background: rgba(255, 255, 255, 0.1);
}
</style>
```

---

### 2.2 修改路由配置

#### 文件路径
`frontend/src/router/index.ts`

#### 添加路由

```typescript
{
  path: '/profile',
  name: 'Profile',
  component: () => import('@/pages/ProfilePage.vue'),
  meta: { requiresAuth: true }
}
```

---

### 2.3 修改 App.vue

#### 文件路径
`frontend/src/App.vue`

#### 修改内容

**1. 修改头像点击事件**

找到以下代码 (大约第 193 行):
```vue
<div class="dock-item" @click="openAvatar">
```

修改为:
```vue
<div class="dock-item" @click="router.push('/profile')">
```

**2. 在移动端抽屉添加个人中心入口**

找到 `mobile-menu-content` 部分 (大约第 244 行),在顶部添加:
```vue
<el-menu-item index="/profile" @click="drawerVisible = false">
  <el-icon><User /></el-icon>
  <span>个人中心</span>
</el-menu-item>
<el-divider />
```

**3. 移除原有的头像弹窗逻辑**

删除或注释掉以下内容:
- `avatarDialog` ref 定义
- `avatarSizeLarge` ref 定义
- `profileForm` ref 定义
- `openAvatar` 函数
- `toggleAvatarSize` 函数
- `saveAvatar` 函数
- `onLogout` 函数
- `avatarDialog` el-dialog 组件

保留 `auth.logout()` 方法,可以添加到个人中心页面。

**4. 导入 useRouter**

在 script 部分添加:
```typescript
import { useRouter } from 'vue-router'

const router = useRouter()
```

---

### 2.4 修改前端 API 封装

#### 文件路径
需要找到实际的 API 文件位置

根据项目结构,可能是:
- `frontend/src/api/index.ts`
- `frontend/src/api/user.ts`
- `frontend/src/api/http.ts`

#### 添加 API 方法

```typescript
// 获取用户统计
export const getUserStats = () => {
  return request.get('/user/stats')
}

// 获取用户信息
export const getUserInfo = () => {
  return request.get('/user/info')
}

// 更新用户信息
export const updateUserInfo = (data) => {
  return request.post('/user/info/update', data)
}

// 上传头像
export const uploadAvatar = (formData) => {
  return request.post('/user/info/upload/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 修改密码
export const updatePassword = (data) => {
  return request.post('/user/info/update/password', data)
}
```

---

## 三、数据库变更

无需数据库变更，所有统计数据基于现有表查询。

---

## 四、测试计划

### 4.1 后端接口测试

1. **测试 /user/stats 接口**
   - 验证统计数据正确性
   - 验证最近活动时间排序正确
   - 验证不同用户数据隔离

### 4.2 前端功能测试

1. **个人信息模块**
   - 头像上传功能
   - 邮箱编辑和保存
   - 手机号编辑和保存
   - 密码修改功能

2. **数据概览模块**
   - 统计数据展示
   - 最近活动时间轴

3. **快捷入口模块**
   - 点击跳转功能

4. **导航切换**
   - 左侧导航栏切换
   - 移动端适配

5. **样式测试**
   - 浅色模式
   - 深色模式
   - 响应式布局

---

## 五、开发顺序

### Phase 1: 后端开发
1. 创建 `UserStatsVo` 类
2. 添加 Repository 查询方法
3. 实现 `getUserStats()` 方法
4. 添加 Controller 接口
5. 测试后端接口

### Phase 2: 前端开发
1. 创建 `ProfilePage.vue` 组件
2. 实现个人信息模块
3. 实现数据概览模块
4. 实现快捷入口模块
5. 添加路由配置
6. 修改 App.vue
7. 封装前端 API
8. 样式优化和测试

### Phase 3: 集成测试
1. 端到端功能测试
2. 样式适配测试
3. Bug 修复

---

## 六、后续扩展方向

### 6.1 学习成就模块 (迭代 1)
- 测验分数趋势图
- 知识点掌握度分布
- 成就徽章系统
- 学习进度可视化

### 6.2 数据导出/备份 (迭代 2)
- 知识库导出 (Markdown/PDF)
- 个人配置备份
- 历史数据清理

### 6.3 个性化设置 (迭代 3)
- 主题自定义
- 界面偏好设置
- 通知设置

---

## 七、更新 AGENTS.md

在 AGENTS.md 的"功能模块详解"章节添加:

### 4.7 个人中心 (Profile)
*   **功能描述**：提供个人信息管理、学习数据概览和快捷访问功能。
*   **核心功能**：
    *   **个人信息**：头像更换、基本信息编辑、密码修改。
    *   **数据概览**：展示已同步文件数、分析结果数、合集数量、标签数量、测验完成数、学习天数等统计数据；展示最近7天的操作时间轴。
    *   **快捷入口**：提供常用功能的快速跳转,包括我的合集、待分析、最近报告、学习测验、标签管理、数据统计、同步历史、系统配置。
*   **前端交互**：`ProfilePage.vue` 采用 macOS 设置风格,左侧导航栏 + 右侧内容区布局,使用卡片式分组展示。
*   **后端接口**：`GET /user/stats` 获取用户统计数据和最近活动。
*   **数据来源**：聚合查询 `data_info`、`analysis_result`、`analysis_collection`、`main_tag`、`sub_tag`、`quiz_record`、`sync_record`、`report_data` 表。

在"更新记录"章节添加:

*   **2026-01-30**: 新增个人中心功能 (`ProfilePage.vue`)，支持个人信息管理、数据概览展示和快捷入口跳转；后端新增 `/user/stats` 接口提供综合统计数据。

---

## 八、注意事项

1. **遵循 AppleStyle 设计规范**
   - 使用 CustomScroll 组件
   - 统一圆角和间距
   - 毛玻璃效果
   - 高饱和度激活态

2. **代码规范**
   - 遵循项目现有代码风格
   - 使用 TypeScript 类型定义
   - 组件化拆分

3. **性能优化**
   - 统计数据查询考虑添加缓存
   - 大量数据考虑分页

4. **用户体验**
   - 加载状态提示
   - 错误处理和提示
   - 平滑动画过渡

---

**文档创建日期**: 2026-01-30

**预计完成时间**: 2-3 个工作日
