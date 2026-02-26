### 10.1 模块概述

**智能推荐模块**是习题模块的核心子功能，整合了两种推荐策略：
1. **复习推荐**（基于艾宾浩斯遗忘曲线）- 推荐需要复习的错题
2. **学习路径推荐**（基于薄弱知识点）- 推荐合适的学习合集

**设计理念**：
- 作为习题模块的默认首页，提升用户学习主动性
- 左右分栏布局，信息密度高但不拥挤
- AppleStyle 设计风格，完整的深色模式适配
- 实时更新推荐数量，引导用户持续学习

### 10.2 前端架构

#### 10.2.1 页面组件

**RecommendationsPage.vue**（`frontend/src/pages/quiz/RecommendationsPage.vue`）

**组件结构**：
```
RecommendationsPage.vue
├── 左侧面板（40%） - 学习路径推荐
│   └── LearningPathRecommender
│       └── RecommendationCard[] (推荐合集卡片列表)
└── 右侧面板（60%） - 复习推荐卡片列表
    └── ReviewCard[] (基于遗忘曲线的复习推荐)
```

**关键 props 和 events**：
```javascript
// emit
@recommendations-loaded(count) // 推荐数据加载完成，传递数量
@start-review(data) // 用户点击开始复习
```

**布局特点**：
- 使用 CSS Grid 布局：`grid-template-columns: 40% 60%`
- 移动端响应式：`@media (max-width: 1024px)` 上下堆叠
- 玻璃态效果：`.glass-panel` 统一样式
- 自定义滚动条：`&::-webkit-scrollbar-thumb`

#### 10.2.2 子组件集成

**LearningPathRecommender.vue**（已存在）
- 功能：基于用户薄弱知识点推荐学习合集
- Props：`limit`（推荐数量），`autoLoad`（是否自动加载）
- API：`api.getCollectionList()` + `api.getUserStats()`

**ReviewCard.vue**（已存在，修复图标导入）
- 功能：展示单条复习推荐卡片
- Props：`recommendation`（推荐数据），`compact`（紧凑模式）
- Events：`@start-review`，`@dismiss`

**推荐数据格式**：
```javascript
{
  mistakeId: Number,           // 错题记录ID
  questionId: Number,          // 题目ID
  questionText: String,        // 题目内容
  questionType: String,        // 题型
  knowledgePoint: String,      // 知识点
  mistakeCount: Number,        // 错误次数
  lastReviewTime: String,      // 最后复习时间（ISO格式）
  nextReviewDate: String,      // 下次复习日期（ISO格式）
  priority: Number             // 推荐优先级（数值越高越优先）
}
```

#### 10.2.3 主页面集成（quiz/index.vue）

**修改内容**：
1. **默认视图切换**：
```javascript
// 修改前：const activeView = ref('history')
// 修改后：
const activeView = ref('recommendations') // 智能推荐为默认视图
```

2. **导航栏新增选项**：
```vue
<el-radio-button value="recommendations">
  智能推荐
  <span v-if="recommendationCount > 0" class="recommendation-badge">
    ({{ recommendationCount }})
  </span>
</el-radio-button>
```

3. **推荐视图内容区**：
```vue
<div v-if="activeView === 'recommendations'" key="recommendations" class="view-container">
  <RecommendationsPage
    @recommendations-loaded="handleRecommendationsLoaded"
    @start-review="handleStartReviewFromRecommendations"
  />
</div>
```

4. **事件处理**：
```javascript
// 推荐数据加载完成
function handleRecommendationsLoaded(count) {
  recommendationCount.value = count
}

// 从推荐页面开始复习
function handleStartReviewFromRecommendations(data) {
  currentMistakeId.value = data.mistakeId
  currentQuestionId.value = data.questionId
  activeView.value = 'mistake' // 跳转到错题详情视图
}
```

5. **推荐数量标记样式**：
```scss
.recommendation-badge {
  margin-left: 4px;
  color: #67c23a; // 成功绿
  font-weight: 600;
  font-size: 12px;
}

.nav-radio-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) .recommendation-badge {
  color: rgba(255, 255, 255, 0.9);
}
```

### 10.3 后端架构

#### 10.3.1 API 接口

**复习推荐接口**（`GET /api/mistake-book/review-recommendation`）

**Controller**（`MistakeBookController.java`）：
```java
@GetMapping("/review-recommendation")
public BaseResponse<List<ReviewRecommendationVO>> getReviewRecommendation() {
    Long userId = securityUtils.getCurrentUserId();
    List<ReviewRecommendationVO> recommendations =
            mistakeBookService.getReviewRecommendations(userId);
    return ResultUtil.success(recommendations);
}
```

#### 10.3.2 业务逻辑

**Service 方法**（`MistakeBookService.java`）

**核心方法**：`getReviewRecommendations(Long userId)`

**算法流程**：
1. 获取用户未掌握的错题列表
2. 批量查询关联的题目信息
3. 计算每个错题的复习优先级：
    - 根据错误次数计算复习间隔（艾宾浩斯遗忘曲线）
    - 计算下次复习时间
    - 计算优先级分数（紧急程度 + 错误次数权重）
4. 按优先级排序
5. 限制返回数量（最多20条）

**艾宾浩斯遗忘曲线实现**：
```java
private Duration calculateReviewInterval(int mistakeCount) {
    // 间隔序列：20分钟、1小时、8小时、1天、2天、6天、15天、30天
    Duration[] intervals = {
        Duration.ofMinutes(20),
        Duration.ofHours(1),
        Duration.ofHours(8),
        Duration.ofDays(1),
        Duration.ofDays(2),
        Duration.ofDays(6),
        Duration.ofDays(15),
        Duration.ofDays(30)
    };

    int index = Math.min(mistakeCount - 1, intervals.length - 1);
    return intervals[Math.max(0, index)];
}
```

**优先级计算算法**：
```java
private int calculatePriority(long daysUntilReview, int mistakeCount) {
    // 紧急程度：已逾期(100) -> 今天到期(80) -> 1天内(60) -> 3天内(40) -> 正常(20)
    int urgencyScore;
    if (daysUntilReview < 0) {
        urgencyScore = 100;
    } else if (daysUntilReview == 0) {
        urgencyScore = 80;
    } else if (daysUntilReview <= 1) {
        urgencyScore = 60;
    } else if (daysUntilReview <= 3) {
        urgencyScore = 40;
    } else {
        urgencyScore = 20;
    }

    // 错误次数权重：每错一次加5分，最多20分
    int mistakeCountWeight = Math.min(mistakeCount * 5, 20);

    return urgencyScore + mistakeCountWeight;
}
```

#### 10.3.3 数据模型

**ReviewRecommendationVO**（`backend/src/main/java/com/review/agent/entity/vo/ReviewRecommendationVO.java`）

**字段说明**：
```java
@Data
@Builder
public class ReviewRecommendationVO {
    private Long mistakeId;              // 错题记录ID
    private Long questionId;             // 题目ID
    private String questionText;         // 题目内容
    private String questionType;         // 题型
    private String knowledgePoint;       // 知识点
    private Integer mistakeCount;        // 错误次数
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime lastReviewTime;    // 最后复习时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime nextReviewDate;    // 下次复习日期
    private Integer priority;            // 推荐优先级（数值越高越优先）
}
```

### 10.4 前端API集成

**http.js**（`frontend/src/api/http.js`）

**新增方法**：
```javascript
/**
 * 获取复习推荐列表
 * 基于遗忘曲线算法返回需要复习的错题
 */
getReviewRecommendation() {
  return request('/mistake-book/review-recommendation')
    .then(data => Array.isArray(data) ? data : [])
    .catch(() => [])
}
```

#### 10.5 交互细节

**加载状态**：使用 `el-skeleton` 组件显示3行骨架屏
**空状态**：使用 `el-empty` 组件，提供"开始做题"快速入口
**导航切换**：使用 `Transition name="fade"` 实现平滑过渡
**数量标记**：实时更新推荐数量，绿色字体突出显示

### 10.6 文件清单

#### 新建文件（1个）
- `frontend/src/pages/quiz/RecommendationsPage.vue` - 智能推荐页面组件

#### 修改文件（7个）

**前端（4个）**：
- `frontend/src/api/http.js` - 添加 `getReviewRecommendation()` 方法
- `frontend/src/pages/quiz/index.vue` - 集成推荐视图、修改默认视图
- `frontend/src/components/quiz/ReviewCard.vue` - 修复图标导入（PriceTag, Close）
- `frontend/src/components/quiz/LearningPathRecommender.vue` - 修复 API 调用（getCollections → getCollectionList）

**后端（3个）**：
- `backend/src/main/java/com/review/agent/entity/vo/ReviewRecommendationVO.java` - 新建VO类
- `backend/src/main/java/com/review/agent/service/MistakeBookService.java` - 实现推荐算法
- `backend/src/main/java/com/review/agent/controller/MistakeBookController.java` - 实现接口

### 10.8 技术亮点

1. **科学的遗忘曲线算法**：基于艾宾浩斯遗忘曲线的复习间隔计算，提升学习效果
2. **智能优先级排序**：结合紧急程度和错误次数，动态调整推荐顺序
3. **优雅的UI设计**：AppleStyle 玻璃态效果，完整的深色模式适配
4. **组件化复用**：复用现有的 ReviewCard 和 LearningPathRecommender 组件
5. **响应式布局**：使用 CSS Grid 实现灵活的左右分栏布局
6. **用户体验优化**：加载状态、空状态、数量标记等细节处理

### 10.9 后续优化建议

1. **性能优化**：
   - 考虑添加推荐结果的缓存（Redis）
   - 大数据量时实现分页加载

2. **功能增强**：
   - 支持手动刷新推荐
   - 支持忽略推荐并记录偏好
   - 支持自定义复习间隔

3. **数据统计**：
   - 记录推荐点击率，优化推荐算法
   - 统计复习完成率，评估学习效果

4. **个性化**：
   - 支持用户自定义复习提醒时间
   - 根据用户习惯调整推荐策略
