# 🧠 Review Agent

> _让每一次向 AI 求助，都成为你技术成长的一块砖_

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

许多开发者已经习惯在遇到问题时第一时间问 AI：“帮我修复这个 Bug”、“分析下报错日志”……
但很少有人回头复盘：**我为什么总在这类问题上卡住？下次还会不会犯同样的错？**

Review Agent 正是为此而生 —— 它会自动扫描你的本地 AI 编程对话记录（如 ChatGPT、Cursor
导出文本），通过智能分析构建专属的「技术认知知识库」，帮你从“用完即忘”走向“持续沉淀”。

📌 **核心理念**：不是替代思考，而是辅助反思。

🛠️ **目标用户**：频繁使用 AI 编程但希望提升长期能力的开发者。

---

## 🚀 特性亮点

| 功能                 | 说明                                 |
|--------------------|------------------------------------|
| 📁 **自动/手动同步**     | 自动扫描指定目录，新增AI聊天记录文件即时入库（仅桌面版）      |
| 🏷️ **智能标签体系**     | 支持主/子标签，避免重复；可手动编辑与合并              |
| 🧩 **结构化知识提取**     | 调用 LLM 提取「问题描述」「根本原因」「解决方案」等字段     |
| 🔄 **状态机管理**       | 分析状态明确：未分析 / 已完成 / 失败（支持重试）        |
| 🔎 **智能问答**        | 基于历史数据与AI聊天问答，巩固之前的学习成果            |
| 🔎 **相似性提醒**       | 新增问题若与历史高度相似，自动提示“类似问题出现过n次”       |
| 🔎 **全文检索 + 标签筛选** | 快速查找过往问题 （全文检索后续支持）                |
| 📅 **日/周报生成**      | 自动生成学习报告，并通过邮件推送（可选）               |
| 🔍 **双端支持**        | 网页版（Web UI） + 本地桌面程序（Windows .exe） |

🎯 适用场景：

- 想系统整理自己常问的 AI 问题
- 发现自身技术盲区和薄弱环节
- 建立个人可检索的技术笔记库

---

## 💻 系统架构概览

```
+------------------+     +------------------+
|   Web Frontend   |     | Desktop Launcher |
|   (Vue + Vite)   |     | (JavaFX WebView) |
+------------------+     +------------------+
           ↓                       ↓
       HTTP API             Localhost API
           ↘                     ↙
        +----------------------------+
        |     Spring Boot Backend    |
        |  • RESTful 接口             |
        |  • 文件扫描与解析            |
        |  • AI 分析调度              |
        |  • 邮件任务                 |
        +----------------------------+
                     ↓
              +-------------+
              | MySQL 8.0+  |
              | (utf8mb4)   |
              +-------------+
```

> ✅ 后端统一服务两个前端入口：网页开发调试 & 桌面客户端运行。

---

## 🛠 环境要求

| 组件         | 版本要求 | 说明                                                                |
|------------|------|-------------------------------------------------------------------|
| JDK        | 17+  | 推荐 [Liberica JDK Full](https://bell-sw.com/)（含 JavaFX 和 jpackage） |
| Node.js    | v18+ | 支持 Vite 构建工具                                                      |
| MySQL      | 8.0+ | 字符集 `utf8mb4`，引擎 `InnoDB`                                         |
| Maven      | 3.8+ | Java 项目构建                                                         |
| npm / pnpm | ^9.0 | 前端依赖管理                                                            |

---

## 🚀 快速开始

### 1. 初始化数据库

```sql
-- 创建数据库
CREATE DATABASE review_agent CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 导入表结构
mysql -u root -p review_agent < backend/src/main/resources/sql/init.sql
```

> 确保脚本成功执行

---

### 2. 配置后端服务

编辑配置文件：
`backend/src/main/resources/application.yml`

> 🔐 敏感信息建议通过环境变量传入，避免提交到 Git。

---

### 3. 启动后端服务

```bash
cd backend
mvn spring-boot:run
```

API 基地址：`http://localhost:8080/api`

---

### 4. 启动前端（网页版）

```bash
cd frontend
npm install
npm run dev
```

🌐 本地访问地址：[http://localhost:5173](http://localhost:5173)

> 默认已配置代理 `/api → http://localhost:8080/api`

---


## 📂 项目结构概览

```bash
review-agent/
├── backend/               # Spring Boot 后端
│   ├── src/main/java/     # 核心逻辑
│   ├── src/main/resources/
│   │   ├── sql/init.sql   # 数据库初始化脚本
│   │   └── application.yml
│
├── frontend/              # Vue 前端
│   ├── src/
│   │   ├── views/         # 页面组件
│   │   ├── api/           # API 请求封装
│   │   └── assets/
│   ├── vite.config.js
│   └── package.json
│
├── launcher/              # JavaFX 桌面启动器
│   ├── src/main/java/com/reviewagent/launcher/
│   │   └── MainView.java  # WebView 容器
│   └── pom.xml
│
├── resources/
│   └── web/               # 存放打包后的前端静态文件
│
└── README.md
```

---

## 📊 使用流程示意

1. 设置日志路径（如 `~/ai_logs`）
2. 同步与AI的对话记录
- 手动导入记录
- 系统自动扫描指定本地目录（桌面端支持）
4. 在 Web 或桌面端查看结构化知识卡片
5. 通过标签分类 + 搜索快速回顾
6. 与AI基于知识库聊天，快速检索/巩固历史问题
7. 每日/周收到一封「本日/周提问总结」邮件

🎯 最终目标：形成一个属于你自己的「认知飞轮」——
**提问 → 解决 → 分析 → 沉淀 → 反思 → 少问**

---

## 🤝 贡献指南

欢迎提交 Issue 或 Pull Request！

🔧 当前待办：

- [ ] 与AI基于知识库聊天（知识库功能开发中）
- [ ] 支持更多 LLM 提供商（OpenAI, Qwen, Gemini）
- [ ] 全文检索
- [ ] 添加练习题推荐模块
- [ ] 实现掌握度动态评分算法

---

## ❤️ 致谢

灵感来源于对现代开发者过度依赖 AI 的反思。
感谢以下生态的支持：

- [Spring AI Alibaba](https://github.com/alibaba/spring-ai-alibaba)

> 如果你觉得这个项目对你有启发，请点个 ⭐ Star 支持一下 ❤️

---
