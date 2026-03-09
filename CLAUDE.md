# CLAUDE.md

使用自动化测试工具（playwright、chormdevtools）时，需要登录的情况，用户名：刘鸿飞，密码：admin123。

> **文档结构**：
> - 本文档 - 快速参考（构建命令、关键约束）
> - [AI-CODE-GUIDE.md](./AI-CODE-GUIDE.md) - 详细编码规范
> - [ARCHITECTURE.md](./ARCHITECTURE.md) - 项目架构详情
>
> This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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

### [L1] Backend - Authentication
| Attribute | Value |
|-----------|-------|
| **Scope** | Backend |
| **Auto-Fixable** | Yes |

Use `SecurityUtils.getCurrentUserId()` - **NEVER** accept userId from Header/Body

```java
// ✅ CORRECT
Long userId = securityUtils.getCurrentUserId();

// ❌ WRONG - Security vulnerability
public void method(@RequestHeader("userId") Long userId)
```

See [AI-CODE-GUIDE.md#rule-be-001](./AI-CODE-GUIDE.md#rule-be-001) for details.

---

### [L1] Backend - Transactions
| Attribute | Value |
|-----------|-------|
| **Scope** | Backend |
| **Auto-Fixable** | Yes |

All write operations must have `@Transactional(rollbackFor = Exception.class)`

```java
@Transactional(rollbackFor = Exception.class)
public void updateData(Long id) {
    // ...
}
```

See [AI-CODE-GUIDE.md#rule-be-002](./AI-CODE-GUIDE.md#rule-be-002) for details.

---

### [L1] Backend - Date Formatting
| Attribute | Value |
|-----------|-------|
| **Scope** | Backend |
| **Auto-Fixable** | Yes |

Use `@JsonFormat` in VO classes - **NEVER** format dates in frontend

```java
@JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
private LocalDateTime createdTime;
```

---

### [L1] Backend - Database Migration
| Attribute | Value |
|-----------|-------|
| **Scope** | Backend |
| **Auto-Fixable** | No |

Use Flyway for database version control

```bash
# Migration files location
backend/src/main/resources/db/migration/

# Naming convention
V1__Initial_schema.sql
V2__Add_user_profile.sql
```

---

### [L1] Frontend - Scroll Components
| Attribute | Value |
|-----------|-------|
| **Scope** | Frontend |
| **Auto-Fixable** | Yes |

MUST use `CustomScroll.vue` or `ScrollStack/` for all scrollable areas

```vue
<template>
  <CustomScroll class="content-scroll">
    <!-- scrollable content -->
  </CustomScroll>
</template>
```

---

### [L1] Frontend - ECharts
| Attribute | Value |
|-----------|-------|
| **Scope** | Frontend |
| **Auto-Fixable** | Yes |

NEVER check container size before init - use ResizeObserver

```javascript
const resizeObserver = new ResizeObserver(() => {
  chartInstance?.resize()
})
resizeObserver.observe(chartRef.value)
```

See [AI-CODE-GUIDE.md#rule-fe-002](./AI-CODE-GUIDE.md#rule-fe-002) for details.

---

### [L1] Frontend - Single Theme
| Attribute | Value |
|-----------|-------|
| **Scope** | Frontend |
| **Auto-Fixable** | Yes |

**System has ONLY ONE fixed dark theme** - No theme switching functionality

- Do NOT use `html.dark` selectors (removed in cleanup)
- Do NOT import or use `useThemeStore` for theme switching
- Charts use fixed dark color values directly
- Theme store (`frontend/src/stores/theme.js`) is kept for compatibility but `isDark` is always `true`

---

### [L1] Frontend - API Response Handling
| Attribute | Value |
|-----------|-------|
| **Scope** | Frontend |
| **Auto-Fixable** | Yes |

`normalizeResponse` unwraps `data` when `code === 0`, components receive direct data

```javascript
// ✅ CORRECT - Access data directly
const data = await knowledgeGraphApi.getSimpleGraph()
if (data && Array.isArray(data.nodes)) {
  nodes.value = data.nodes
}

// ❌ WRONG - Don't check code or access .data
const res = await knowledgeGraphApi.getSimpleGraph()
if (res.code === 200) {        // Error: code is undefined
  nodes.value = res.data.nodes // Error: data is undefined
}
```

**Note**: See `frontend/src/api/base.js` → `normalizeResponse()` for implementation details. When `code !== 0`, an Error is thrown with `message`.

---

### [L1] Document Location Rules
| Attribute | Value |
|-----------|-------|
| **Scope** | All |
| **Auto-Fixable** | No |

**Root directory only allows**: README.md, CLAUDE.md, CHANGELOG.md

**All other documents must be placed under `docs/`** with naming convention:
```
YYYY-MM-DD-CASE-NNN-DESCRIPTION_VERSION.md

Examples:
✅ 2026-01-15-CASE-001-memory-leak-analysis_01.md
❌ temp.md (no date/case number)
```

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
```

---

## Scenario Index: When I...

| When I... | Check These Rules | See Also |
|-----------|------------------|----------|
| **Write Backend Service methods** | [L1] Authentication, [L1] Transactions | AI-CODE-GUIDE.md |
| **Write Backend Controller** | [L1] Authentication | AI-CODE-GUIDE.md |
| **Create new VO/DTO classes** | [L1] Date Formatting | AI-CODE-GUIDE.md |
| **Create database migrations** | [L1] Database Migration | AI-CODE-GUIDE.md |
| **Write Vue components** | [L1] Scroll Components, [L1] Single Theme | AI-CODE-GUIDE.md |
| **Use ECharts** | [L1] ECharts | AI-CODE-GUIDE.md |
| **Call API from frontend** | [L1] API Response Handling | AI-CODE-GUIDE.md |
| **Create documentation** | [L1] Document Location Rules | AI-CODE-GUIDE.md |
| **Understand module relationships** | - | ARCHITECTURE.md |
| **Check database schema** | - | ARCHITECTURE.md |

---

## L1/L2/L3 Level Definitions

| Level | Description | Compliance |
|-------|-------------|------------|
| **L1** | Must follow | Violation causes bugs or security issues |
| **L2** | Strongly recommended | Exceptions need comments explaining why |
| **L3** | Best practice | Optimization suggestions |

---

## File Structure Reference

```
backend/
├── src/main/java/
│   ├── controller/      # REST API endpoints
│   ├── service/         # Business logic
│   ├── repository/      # Database access
│   ├── entity/          # JPA entities
│   ├── vo/              # Value objects (API response)
│   └── config/          # Configuration classes
├── src/main/resources/
│   └── db/migration/    # Flyway migrations
└── pom.xml

frontend/
├── src/
│   ├── api/             # API client
│   ├── components/      # Vue components
│   ├── pages/           # Page components
│   ├── stores/          # Pinia stores
│   ├── router/          # Vue Router
│   └── styles/          # SCSS styles
└── package.json
```
