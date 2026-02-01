# 练习题功能 API 测试指南

## 测试环境
- **Base URL**: `http://localhost:8080/api`
- **认证**: 需要有效的 JWT Token
- **准备条件**:
  1. MySQL 数据库已执行迁移脚本
  2. 后端服务已启动
  3. 存在有效的测试合集

---

## 测试前置准备

### 1. 获取认证 Token
```bash
# 登录获取 JWT Token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test",
    "password": "123456"
  }'

# 保存返回的 token 用于后续请求
# 示例返回: {"code": 200, "data": {"token": "eyJhbGci...", ...}}
```

### 2. 创建测试合集
```bash
# 创建一个测试合集
curl -X POST http://localhost:8080/api/collection \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "name": "测试合集",
    "description": "用于测试练习题功能的合集"
  }'

# 保存返回的 collectionId
# 示例返回: {"code": 200, "data": {"id": 1, ...}}
```

### 3. 获取现有合集（使用第一个）
```bash
curl -X GET http://localhost:8080/api/collection/list \
  -H "Authorization: Bearer YOUR_TOKEN"

# 使用返回的第一个合集 ID
```

---

## 测试 1: Quiz API - 多题型生成

### 1.1 生成测验（验证多题型支持）
```bash
curl -X POST http://localhost:8080/api/collection/generate-quiz \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "collectionId": COLLECTION_ID
  }'
```

**预期结果**:
- ✅ HTTP 200
- ✅ 返回 `QuizVo` 包含：
  - `id`: 测验 ID
  - `collectionId`: 合集 ID
  - `questions`: 题目数组（3-5 个题目）
- ✅ 题目应包含多种类型（取决于 LLM 生成）
- ✅ 每个题目包含：
  - `id`: 题目 ID
  - `question`: 题目文本
  - `options`: 选项数组（单选/多选）
  - `answer`: 正确答案
  - `explanation`: 解析
  - `userAnswer`: 用户答案（初始为 null）
  - `type`: 题目类型（单选、多选、判断、填空、代码识别）
  - `knowledgePoint`: 知识点
  - `difficulty`: 难度等级 (1-5)
  - `timeLimit`: 答题时限（秒）

**验证点**:
- [ ] 返回的题目数量在 3-5 之间
- [ ] 每个题目有正确的 JSON 结构
- [ ] 题目类型字段存在且有效
- [ ] 难度等级在 1-5 之间
- [ ] 答题时限合理（30-120 秒）

---

## 测试 2: Quiz API - 提交答案（多题型支持）

### 2.1 提交单选题答案
```bash
curl -X POST http://localhost:8080/api/collection/submit-answer \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "questionId": QUESTION_ID,
    "userAnswer": "A"
  }'
```

### 2.2 提交多选题答案
```bash
curl -X POST http://localhost:8080/api/collection/submit-answer \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "questionId": QUESTION_ID,
    "userAnswer": "A,B"
  }'
```

### 2.3 提交判断题答案
```bash
curl -X POST http://localhost:8080/api/collection/submit-answer \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "questionId": QUESTION_ID,
    "userAnswer": "true"
  }'
```

### 2.4 提交填空题答案
```bash
curl -X POST http://localhost:8080/api/collection/submit-answer \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "questionId": QUESTION_ID,
    "userAnswer": "正确答案内容"
  }'
```

**预期结果**:
- ✅ HTTP 200
- ✅ 返回 `BaseResponse<Void>` 成功响应

**数据库验证**:
```sql
-- 验证 quiz_question 表
SELECT id, question_type, correct_answer, user_answer, is_correct, answer_count, correct_count
FROM quiz_question
WHERE id = QUESTION_ID;

-- 预期结果：
-- user_answer = 提交的答案
-- is_correct = 1（如果正确）或 0（如果错误）
-- answer_count = 原值 + 1
-- correct_count = 原值 + 1（如果正确）
```

---

## 测试 3: Quiz API - 重置测验

```bash
curl -X POST http://localhost:8080/api/collection/reset \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "quizId": QUIZ_ID
  }'
```

**预期结果**:
- ✅ HTTP 200
- ✅ 返回 `BaseResponse<Void>` 成功响应

**数据库验证**:
```sql
-- 验证 quiz_question 表
SELECT id, user_answer, is_correct
FROM quiz_question
WHERE quiz_id = QUIZ_ID;

-- 预期结果：
-- user_answer = NULL（所有题目）
-- is_correct = NULL（所有题目）
```

---

## 测试 4: 数据库表验证

### 4.1 验证 quiz_question 表扩展字段
```sql
-- 检查新增字段是否存在
DESCRIBE quiz_question;

-- 预期包含的字段：
-- question_type VARCHAR(20)
-- difficulty_level TINYINT
-- knowledge_point VARCHAR(100)
-- time_limit INT
-- answer_count INT
-- correct_count INT
```

### 4.2 验证 quiz_mistake 表
```sql
-- 检查表是否存在且结构正确
DESCRIBE quiz_mistake;

-- 预期字段：
-- id BIGINT (PK)
-- user_id BIGINT
-- question_id BIGINT
-- quiz_id BIGINT
-- mistake_count INT
-- last_mistake_time DATETIME
-- mastered BOOLEAN
-- created_time DATETIME
-- updated_time DATETIME
```

### 4.3 验证 knowledge_mastery 表
```sql
-- 检查表是否存在且结构正确
DESCRIBE knowledge_mastery;

-- 预期字段：
-- id BIGINT (PK)
-- user_id BIGINT
-- knowledge_point VARCHAR(100)
-- total_answered INT
-- correct_count INT
-- mastery_score DECIMAL(5,2)
-- average_time INT
-- last_practice_time DATETIME
-- created_time DATETIME
-- updated_time DATETIME
```

---

## 测试 5: 错题本功能（需要创建 Controller）

### 5.1 创建 MistakeBookController
**需要在 `backend/src/main/java/com/review/agent/controller/` 创建：**

```java
@RestController
@RequestMapping("/mistake-book")
public class MistakeBookController {

    @Resource
    private MistakeBookService mistakeBookService;

    // 获取用户错题列表
    @GetMapping("/list")
    public BaseResponse<List<QuizQuestion>> getUserMistakes() {
        return ResultUtil.success(mistakeBookService.getUserMistakes());
    }

    // 获取未掌握的错题
    @GetMapping("/unmastered")
    public BaseResponse<List<Mistake>> getUnmasteredMistakes() {
        return ResultUtil.success(mistakeBookService.getUnmasteredMistakes());
    }

    // 获取复习推荐（基于遗忘曲线）
    @GetMapping("/review-recommendation")
    public BaseResponse<List<Mistake>> getReviewRecommendation() {
        return ResultUtil.success(mistakeBookService.getReviewRecommendation());
    }

    // 标记错题为已掌握
    @PostMapping("/mark-mastered/{questionId}")
    public BaseResponse<Void> markAsMastered(@PathVariable Long questionId) {
        mistakeBookService.markAsMastered(questionId);
        return ResultUtil.success();
    }

    // 获取错题统计
    @GetMapping("/stats")
    public BaseResponse<Map<String, Object>> getMistakeStats() {
        return ResultUtil.success(mistakeBookService.getMistakeStats());
    }
}
```

**创建文件**: `backend/src/main/java/com/review/agent/controller/MistakeBookController.java`

---

## 测试 6: 知识点掌握度功能（需要创建 Controller）

### 6.1 创建 KnowledgeMasteryController
**需要在 `backend/src/main/java/com/review/agent/controller/` 创建：**

```java
@RestController
@RequestMapping("/knowledge-mastery")
public class KnowledgeMasteryController {

    @Resource
    private KnowledgeMasteryService knowledgeMasteryService;

    // 获取用户知识图谱
    @GetMapping("/map")
    public BaseResponse<List<KnowledgeMastery>> getUserKnowledgeMap() {
        return ResultUtil.success(knowledgeMasteryService.getUserKnowledgeMap());
    }

    // 获取薄弱知识点
    @GetMapping("/weak")
    public BaseResponse<List<KnowledgeMastery>> getWeakKnowledgePoints(
            @RequestParam(defaultValue = "5") int limit) {
        return ResultUtil.success(knowledgeMasteryService.getWeakKnowledgePoints(limit));
    }

    // 获取掌握度低于阈值的知识点
    @GetMapping("/below-threshold")
    public BaseResponse<List<KnowledgeMastery>> getBelowThreshold(
            @RequestParam(defaultValue = "60") BigDecimal threshold) {
        return ResultUtil.success(knowledgeMasteryService.getBelowThreshold(threshold));
    }

    // 获取掌握度统计
    @GetMapping("/stats")
    public BaseResponse<Map<String, Object>> getMasteryStats() {
        return ResultUtil.success(knowledgeMasteryService.getMasteryStats());
    }

    // 获取最近练习的知识点
    @GetMapping("/recent")
    public BaseResponse<List<KnowledgeMastery>> getRecentlyPracticed(
            @RequestParam(defaultValue = "10") int limit) {
        return ResultUtil.success(knowledgeMasteryService.getRecentlyPracticed(limit));
    }
}
```

**创建文件**: `backend/src/main/java/com/review/agent/controller/KnowledgeMasteryController.java`

---

## 测试 7: 错题本 API 测试

### 7.1 获取用户错题列表
```bash
curl -X GET http://localhost:8080/api/mistake-book/list \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**预期结果**:
- ✅ HTTP 200
- ✅ 返回用户的所有错题（包含题目详情）

**数据库验证**:
```sql
-- 验证 quiz_mistake 表有数据
SELECT COUNT(*) FROM quiz_mistake WHERE user_id = CURRENT_USER_ID;

-- 预期结果：
-- 如果之前答错过题目，COUNT > 0
```

### 7.2 获取未掌握的错题
```bash
curl -X GET http://localhost:8080/api/mistake-book/unmastered \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**预期结果**:
- ✅ HTTP 200
- ✅ 返回 mastered = false 的错题

---

## 测试 8: 知识点掌握度 API 测试

### 8.1 获取用户知识图谱
```bash
curl -X GET http://localhost:8080/api/knowledge-mastery/map \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**预期结果**:
- ✅ HTTP 200
- ✅ 返回用户的所有知识点掌握度记录
- ✅ 每个记录包含：
  - `knowledgePoint`: 知识点名称
  - `totalAnswered`: 总答题次数
  - `correctCount`: 正确次数
  - `masteryScore`: 掌握度（0-100）

**数据库验证**:
```sql
-- 验证 knowledge_mastery 表
SELECT knowledge_point, mastery_score, total_answered, correct_count
FROM knowledge_mastery
WHERE user_id = CURRENT_USER_ID;

-- 预期结果：
-- 如果答题后，应存在记录
-- mastery_score 在 0-100 之间
```

### 8.2 获取薄弱知识点
```bash
curl -X GET "http://localhost:8080/api/knowledge-mastery/weak?limit=5" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**预期结果**:
- ✅ HTTP 200
- ✅ 返回掌握度最低的 5 个知识点
- ✅ 返回的知识点 `totalAnswered >= 3`（已验证过）

---

## 测试 9: 业务逻辑验证

### 9.1 错题本逻辑验证

**场景 1: 首次答错**
1. 选择一个题目，提交错误答案
2. 检查 `quiz_mistake` 表

```sql
SELECT * FROM quiz_mistake WHERE question_id = QUESTION_ID;

-- 预期：
-- mistake_count = 1
-- mastered = false
-- last_mistake_time = 当前时间
```

**场景 2: 重复答错**
1. 对同一题目再次提交错误答案
2. 检查 `quiz_mistake` 表

```sql
SELECT * FROM quiz_mistake WHERE question_id = QUESTION_ID;

-- 预期：
-- mistake_count = 2（累加）
-- mastered = false
-- last_mistake_time = 更新为当前时间
```

**场景 3: 答对后查看**
1. 提交正确答案
2. 查询错题本

```sql
SELECT * FROM quiz_mistake WHERE question_id = QUESTION_ID;

-- 预期：
-- 记录仍存在（不会自动删除，需要通过 markAsMastered 标记）
-- mastered 保持为 false（需要连续答对 3 次才标记）
```

**场景 4: 遗忘曲线复习推荐**
1. 等待 1 天
2. 调用复习推荐 API

```bash
curl -X GET http://localhost:8080/api/mistake-book/review-recommendation \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**预期**:
- 返回 1 天前答错的错题
- 按照 `last_mistake_time` 排序

---

### 9.2 知识点掌握度逻辑验证

**场景 1: 首次答题**
1. 选择一个包含知识点的题目并提交答案
2. 检查 `knowledge_mastery` 表

```sql
SELECT * FROM knowledge_mastery
WHERE user_id = CURRENT_USER_ID AND knowledge_point = 'Java';

-- 预期：
-- total_answered = 1
-- correct_count = 0 或 1（根据答案是否正确）
-- mastery_score = 20（如果错误）或 60（如果正确）
-- last_practice_time = 当前时间
```

**场景 2: 连续答对**
1. 对同一知识点的多个题目连续答对 3 次
2. 检查掌握度变化

```sql
SELECT * FROM knowledge_mastery
WHERE user_id = CURRENT_USER_ID AND knowledge_point = 'Java';

-- 预期：
-- total_answered 累加
-- correct_count 累加
-- mastery_score 递增（最终可能超过 80）
-- 可能触发连续正确奖励（额外 +2 分）
```

**场景 3: 答题速度影响**
1. 快速答题（≤ 平均时间 1.5 倍）
2. 缓慢答题（> 平均时间 2 倍）
3. 观察掌握度变化

**预期**:
- 快速答题：`mastery_score` 额外加分（timeWeight = 1.2）
- 缓慢答题：`mastery_score` 扣分（timeWeight = 0.8）

---

## 测试 10: 性能和稳定性测试

### 10.1 并发测试
```bash
# 使用 Apache Bench 或类似工具
ab -n 100 -c 10 -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/collection/generate-quiz

# 预期：无 500 错误，响应时间合理
```

### 10.2 数据一致性测试
```bash
# 多次提交同一题目的答案
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/collection/submit-answer \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer YOUR_TOKEN" \
    -d "{\"questionId\": $QUESTION_ID, \"userAnswer\": \"A\"}"
done

# 验证：answer_count 应为 5
```

---

## 测试 11: 边界情况测试

### 11.1 无效题目类型
```bash
# 尝试直接向数据库插入无效类型（通过数据库直接操作）
INSERT INTO quiz_question (quiz_id, question_text, question_type, ...)
VALUES (..., 'invalid_type', ...);

# 预期：
-- 如果应用尝试解析，应该优雅降级到 SINGLE_CHOICE
```

### 11.2 极端答案格式
```bash
# 提交空的或异常格式答案
curl -X POST http://localhost:8080/api/collection/submit-answer \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "questionId": QUESTION_ID,
    "userAnswer": ""
  }'

# 预期：
-- HTTP 400 或优雅处理
-- 不应该导致 500 错误
```

### 11.3 超长知识点名称
```bash
# 提交超过 100 字符的知识点（通过数据库）
UPDATE knowledge_mastery
SET knowledge_point = '...超长字符串...'
WHERE user_id = ...;

# 预期：
-- 数据库应该截断或拒绝（VARCHAR(100) 限制）
```

---

## 测试 12: 完整端到端流程

### 12.1 从生成到完成
```bash
# Step 1: 登录
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "test", "password": "123456"}' \
  | jq -r '.data.token')

# Step 2: 生成测验
QUIZ_ID=$(curl -s -X POST http://localhost:8080/api/collection/generate-quiz \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"collectionId\": $COLLECTION_ID}" \
  | jq -r '.data.id')

# Step 3: 提交答案（循环所有题目）
# 获取题目并逐个提交（这里简化为第一个）
curl -s -X GET http://localhost:8080/api/collection/get-questions/$QUIZ_ID \
  -H "Authorization: Bearer $TOKEN" \
  | jq -r '.data[0]'

# 提交答案
curl -X POST http://localhost:8080/api/collection/submit-answer \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"questionId\": $QUESTION_ID, \"userAnswer\": \"A\"}"

# Step 4: 验证错题本（如果答错）
curl -s -X GET http://localhost:8080/api/mistake-book/list \
  -H "Authorization: Bearer $TOKEN"

# Step 5: 验证知识点掌握度
curl -s -X GET http://localhost:8080/api/knowledge-mastery/map \
  -H "Authorization: Bearer $TOKEN"
```

**预期流程**:
- ✅ 每个步骤返回 200
- ✅ 答案正确时，错题本不增加记录
- ✅ 答案错误时，错题本增加记录
- ✅ 知识点掌握度正确更新

---

## 📝 测试检查清单

### 基础功能
- [ ] Quiz API 正常响应
- [ ] 多题型生成成功
- [ ] 答案提交功能正常
- [ ] 测验重置功能正常

### 错题本功能
- [ ] 错题自动记录
- [ ] 重复错误次数累加
- [ ] 遗忘曲线推荐正常
- [ ] 错题列表查询正常
- [ ] 掌握状态标记正常

### 知识点掌握度
- [ ] 掌握度自动计算
- [ ] 加权算法生效
- [ ] 薄弱点识别正确
- [ ] 统计数据准确

### 数据库验证
- [ ] quiz_question 扩展字段存在
- [ ] quiz_mistake 表结构正确
- [ ] knowledge_mastery 表结构正确
- [ ] 外键约束生效
- [ ] 索引优化生效

### 性能和稳定性
- [ ] 并发请求无崩溃
- [ ] 数据一致性保证
- [ ] 边界情况优雅处理

---

**创建时间**: 2026-02-01
**测试人员**: 开发团队
**版本**: v1.0 - 练习题功能增强
