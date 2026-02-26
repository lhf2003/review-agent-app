# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **Documentation Structure**:
> - [AI-CODE-GUIDE.md](./AI-CODE-GUIDE.md) - Detailed coding standards with templates and anti-patterns
> - [ARCHITECTURE.md](./ARCHITECTURE.md) - Project architecture, modules, and data models
> - This file - Quick reference for build commands and key constraints

---

## Build & Run Commands

### Backend (Java/Spring Boot)
```bash
cd backend
mvn spring-boot:run      # Run the application (requires MySQL and Redis)
mvn test                 # Run tests
mvn test -Dtest=QuizServiceTest  # Run specific test
mvn clean package        # Package as JAR
```

### Frontend (Vue 3 + Vite)
```bash
cd frontend
npm install              # Install dependencies (first time only)
npm run dev              # Development server
npm run build            # Build for production
npm run preview          # Preview production build
```

### Launch Module (JavaFX Desktop)
```bash
cd launch
mvn javafx:run           # Run the JavaFX application
```

---

## Quick Constraints Reference

### Backend - Authentication
**Rule**: Use `SecurityUtils.getCurrentUserId()` - NEVER accept userId from Header/Body
```java
// ✅ CORRECT
Long userId = securityUtils.getCurrentUserId();

// ❌ WRONG - Security vulnerability
public void method(@RequestHeader("userId") Long userId)
```
See [AI-CODE-GUIDE.md#rule-be-001](./AI-CODE-GUIDE.md#rule-be-001) for full details.

### Backend - Transactions
**Rule**: All write operations must have `@Transactional(rollbackFor = Exception.class)`
```java
@Transactional(rollbackFor = Exception.class)
public void updateData(Long id) {
    // ...
}
```

### Backend - Date Formatting
**Rule**: Use `@JsonFormat` in VO classes - NEVER format dates in frontend
```java
@JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
private LocalDateTime createdTime;
```

### Frontend - Scroll Components
**Rule**: MUST use `CustomScroll.vue` or `ScrollStack/` for all scrollable areas
```vue
<template>
  <CustomScroll class="content-scroll">
    <!-- scrollable content -->
  </CustomScroll>
</template>
```

### Frontend - ECharts
**Rule**: NEVER check container size before init - use ResizeObserver
See [AI-CODE-GUIDE.md#rule-fe-001](./AI-CODE-GUIDE.md#rule-fe-001) for the complete template.

### Frontend - Dark Mode
**Rule**: Use `html.dark` selector (NOT `:global(.dark)` or `@media`)
```scss
html.dark .my-component {
  background: rgba(28, 28, 30, 0.75);
}
```

---

## Project Architecture Overview

**Review Agent** - A knowledge management tool that parses AI chat logs, extracts structured insights via LLM analysis, and provides quiz-based learning features.

### Component Diagram
```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   Frontend  │──────│   Backend    │──────│  MySQL/     │
│  (Vue 3)    │ JWT  │ (Spring Boot)│      │  Redis      │
└─────────────┘      └──────────────┘      └─────────────┘
                            │
                            │ LLM API
                            ▼
                    ┌──────────────┐
                    │ OpenAI/      │
                    │ Gemini/      │
                    │ Ollama/      │
                    │ DashScope    │
                    └──────────────┘
```

### Core Modules
1. **Sync & Data** - File scanning and metadata management
2. **Analysis** - LLM-powered content analysis (DataAnalysisNode, TagClassifyNode)
3. **Tags & Collections** - Two-level tag system and collection management
4. **Quiz** - AI-generated questions and answer recording
5. **Mistake Book** - Wrong answer tracking with Ebbinghaus forgetting curve
6. **Smart Recommendations** - Review recommendations based on forgetting curve
7. **Achievements** - Learning progress and achievement system

See [ARCHITECTURE.md](./ARCHITECTURE.md) for detailed module index and data models.

---

## Environment Requirements

- **JDK 17+**
- **Node.js v22+**
- **MySQL 8.0+** (database: `review_agent`)
- **Redis**

### Optional Environment Variables
```bash
DB_USERNAME=root          # MySQL username (default: root)
DB_PASSWORD=123456        # MySQL password (default: 123456)
DASHSCOPE_API_KEY=xxx     # Alibaba DashScope API key
JWT_SECRET=xxx            # JWT signing secret (min 256 bits)
OLLAMA_BASE_URL=xxx       # Local Ollama config
OLLAMA_CHAT_MODEL=xxx
```

---

## Document Location Rules

**Root directory only allows**:
- `README.md` - Project documentation for users
- `CLAUDE.md` - This file, Claude Code quick reference
- `AI-CODE-GUIDE.md` - Detailed coding standards
- `ARCHITECTURE.md` - Architecture documentation
- `CHANGELOG.md` - Project changelog

**All other documents must be placed under `docs/`** with naming convention:
```
YYYY-MM-DD-CASE-NNN-DESCRIPTION_VERSION.md

Examples:
✅ 2026-01-15-CASE-001-memory-leak-analysis_01.md
✅ 2026-02-12-CASE-001-echarts-resize-fix_01.md
❌ temp.md (no date/case number)
```

See [AI-CODE-GUIDE.md#rule-doc-001](./AI-CODE-GUIDE.md#rule-doc-001) for full document specifications.

---

## Quick Links

| Need | Document |
|------|----------|
| Detailed coding rules with templates | [AI-CODE-GUIDE.md](./AI-CODE-GUIDE.md) |
| Architecture & data models | [ARCHITECTURE.md](./ARCHITECTURE.md) |
| Build commands | This file (above) |
| Module dependencies | [ARCHITECTURE.md#模块索引](./ARCHITECTURE.md#模块索引) |
| Database schema | [ARCHITECTURE.md#数据模型](./ARCHITECTURE.md#数据模型) |
