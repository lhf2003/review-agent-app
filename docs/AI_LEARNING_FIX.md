# AI 学习辅导功能修复指南

## 🔧 已修复的问题

**错误信息：**
```
No enum constant com.review.agent.entity.pojo.QuestionType.single_choice
```

**根本原因：**
- `@Enumerated(EnumType.STRING)` 使用枚举名称（`SINGLE_CHOICE`）存储到数据库
- 但代码中使用 `fromCode("single_choice")` 传入的是 code 值
- JPA 从数据库读取时，尝试调用 `QuestionType.valueOf("single_choice")` 失败

## ✅ 修复方案

### 1. 添加了自定义转换器
创建了 `QuestionTypeConverter.java`，实现了：
- 数据库存储：code 值（如 `single_choice`）
- 数据库读取：code 值转枚举
- 自动应用：`@Converter(autoApply = true)`

### 2. 更新了实体定义
修改了 `QuizQuestion.java`：
- 移除：`@Enumerated(EnumType.STRING)`
- 添加：`@Convert(converter = QuestionTypeConverter.class)`

### 3. 数据迁移脚本
创建了 `fix_question_type.sql`，用于更新现有数据。

## 📋 应用步骤

### 方式一：手动执行 SQL（推荐）

1. **连接到 MySQL 数据库**
```bash
mysql -u root -p review_agent
```

2. **执行迁移脚本**
```sql
source backend/src/main/resources/sql/fix_question_type.sql
```

3. **重启后端服务**
```bash
cd backend
mvn spring-boot:run
```

### 方式二：让后端自动处理（如果已实现 Flyway/Liquibase）

重启后端服务，迁移脚本会自动执行。

## 🧪 测试验证

1. **访问合集详情页面**
2. **点击"AI 学习辅导"按钮**
3. **查看控制台**，应该不再出现 `No enum constant` 错误
4. **验证题目生成**是否成功

## 📝 修复后的行为

### 存储格式
- **修复前**：`question_type = 'SINGLE_CHOICE'`（枚举名）
- **修复后**：`question_type = 'single_choice'`（code 值）

### 代码使用
```java
// 设置时 - 使用 code 值
qq.setQuestionType(QuestionType.fromCode("single_choice"));

// 读取时 - 转换器自动处理
QuestionType type = quizQuestion.getQuestionType(); // 自动从 code 转换
```

## 🔍 故障排查

如果问题仍然存在：

1. **检查数据库数据**
```sql
SELECT question_type FROM review_agent.quiz_question;
```
应该看到小写的值（如 `single_choice`），而不是大写的枚举名（如 `SINGLE_CHOICE`）

2. **检查后端日志**
```bash
# 查找是否有其他错误
tail -f backend/logs/spring.log | grep -i "question.*type"
```

3. **清理并重新编译**
```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

## 📚 相关文件

- `QuestionType.java` - 枚举定义
- `QuestionTypeConverter.java` - JPA 转换器
- `QuizQuestion.java` - 实体类
- `fix_question_type.sql` - 迁移脚本
- `CollectionDetailPage.vue` - 前端页面（已改进错误处理）

---

**创建时间**: 2026-02-01
**修复分支**: phase2-quiz-enhancements
**状态**: ✅ 已修复并提交
