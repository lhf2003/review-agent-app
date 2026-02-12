# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

### Backend (Java/Spring Boot)
```bash
# Navigate to backend directory
cd backend

# Run the application (requires MySQL and Redis running)
mvn spring-boot:run

# Run tests
mvn test

# Run a specific test class
mvn test -Dtest=QuizServiceTest

# Package as JAR
mvn clean package
```

### Frontend (Vue 3 + Vite)
```bash
# Navigate to frontend directory
cd frontend

# Install dependencies (first time only)
npm install

# Development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Launch Module (JavaFX Desktop)
```bash
# Navigate to launch directory
cd launch

# Run the JavaFX application
mvn javafx:run
```

## Project Architecture

**Review Agent** is a knowledge management tool that parses AI chat logs, extracts structured insights via LLM analysis, and provides quiz-based learning features.

### Component Architecture

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

### Backend Module Structure

- **`config/`** - Spring configuration classes:
  - `MultiLLMConfig` - Multiple LLM provider support (OpenAI, Gemini, DashScope, Ollama, DeepSeek)
  - `SecurityConfig` - JWT authentication setup
  - `ThreadPoolConfig` - Dedicated thread pools for analysis/SSE tasks
  - `RedissonConfig` - Redis client configuration
  - `VectorStoreConfig` - Vector embeddings for similarity search

- **`graph/`** - Analysis orchestration using node-based pipeline:
  - `nodes/` - Analysis nodes (DataAnalysisNode, SessionExtractionNode, TagClassifyNode)
  - `dispatcher/` - Node execution dispatchers
  - `hook/` - Pre/post processing hooks
  - `interceptor/` - Execution interceptors

- **`controller/`** - REST API endpoints

- **`service/`** - Core business logic with transaction boundaries

- **`repository/`** - JPA data access layer

- **`entity/`** - Data models organized as:
  - `pojo/` - JPA entities (database tables)
  - `vo/` - View objects (API responses)
  - `dto/` - Data transfer objects
  - `request/` - API request payloads

- **`common/`** - Shared utilities:
  - `security/` - JWT filter, rate limiting
  - `sse/` - SSE connection management
  - `utils/` - SecurityUtils for getting current user ID
  - `exception/` - Global exception handling

### Frontend Module Structure

- **`api/`** - Axios HTTP client and endpoint definitions

- **`components/`** - Reusable Vue components:
  - `CustomScroll.vue` - **REQUIRED** for all scrollable areas (AppleStyle)
  - `ScrollStack/` - Alternative scroll component
  - `quiz/` - Quiz question renderer components

- **`pages/`** - Route-level page components

- **`stores/`** - Pinia state management (auth, chat, theme)

- **`router/`** - Vue Router configuration

### Launch Module

JavaFX desktop application (`launch/`) that wraps the frontend in a WebView, providing local file system access for scanning/importing chat logs.

## Key Development Constraints

### Authentication Pattern
- **Frontend**: Send only `Authorization: Bearer <JWT>` header
- **Backend**: DO NOT accept `userId` as request header. Use `SecurityUtils.getCurrentUserId()` to get authenticated user ID from Security Context
- Example:
  ```java
  Long userId = securityUtils.getCurrentUserId();
  ```

### Soft Delete Convention
Core entities use soft delete pattern with fields:
- `deleted` (Boolean)
- `deleted_at` (LocalDateTime)
- Repository provides `softDelete()`, `hardDelete()`, and `restore()` methods
- Queries automatically filter deleted records

### Transaction Boundaries
Service methods that perform write operations must include:
```java
@Transactional(rollbackFor = Exception.class)
```

### Frontend AppleStyle Design
- **Mandatory**: Use `CustomScroll.vue` or `ScrollStack/` components for all scrollable areas
- Design tokens: Glassmorphism, 16px border-radius, backdrop-filter blur
- Animation curve: `cubic-bezier(0.25, 1, 0.5, 1)`
- No default browser focus outlines (globally disabled)

### Frontend ECharts Integration Best Practices

**CRITICAL**: Chart components MUST NOT check container size before initialization.

**Problem Pattern (DO NOT USE)**:
```javascript
// ❌ WRONG: Causes charts to never initialize
function initChart() {
  if (chartRef.value.clientWidth === 0 || chartRef.value.clientHeight === 0) {
    return  // ← This blocks initialization permanently
  }
  chartInstance = echarts.init(chartRef.value, theme)
}
```

**Why This Fails**:
- Vue 3 component lifecycle: DOM element exists but layout not complete
- Container size is 0x0 when `onMounted` executes
- Even with `requestAnimationFrame` or `setTimeout`, timing is unpredictable
- Once initialization is skipped, `chartInstance` remains `null` forever
- Subsequent data updates cannot render because chart was never created

**Correct Pattern (MUST USE)**:
```javascript
// ✅ CORRECT: Allow ECharts to initialize at any size
let resizeObserver = null  // Global variable

function initChart() {
  if (!chartRef.value) return

  if (chartInstance) {
    chartInstance.dispose()
  }

  // Initialize immediately without size check
  const theme = themeStore.isDark ? 'dark' : undefined
  chartInstance = echarts.init(chartRef.value, theme, {
    backgroundColor: 'transparent',
    renderer: 'canvas'
  })

  // Stop previous observer
  if (resizeObserver) {
    resizeObserver.disconnect()
  }

  // Monitor container size changes
  resizeObserver = new ResizeObserver((entries) => {
    for (let entry of entries) {
      const { width, height } = entry.contentRect
      if (width > 0 && height > 0 && chartInstance) {
        chartInstance.resize()  // Auto-resize when container becomes available
      }
    }
  })

  resizeObserver.observe(chartRef.value)

  updateChart()
  emit('chart-ready', chartInstance)
}

function updateChart() {
  // Auto-initialize if not yet created
  if (!chartInstance) {
    initChart()
    return
  }

  if (!hasData.value) return

  // ... chart configuration
  chartInstance.setOption(option, true)
}

onUnmounted(() => {
  // Clean up observer
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }

  // Dispose chart
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
    emit('chart-dispose')
  }
})
```

**Why This Works**:
1. ECharts can initialize on 0x0 containers (official support)
2. ResizeObserver detects when container becomes available
3. Auto-resize ensures chart renders correctly
4. Observer cleanup prevents memory leaks
5. Multiple fallback mechanisms ensure reliability

**Affected Components** (fixed 2026-02-12):
- `KnowledgeRadarChart.vue`
- `ScoreTrendChart.vue`
- `TimeDistributionChart.vue`
- `LearningHeatmapChart.vue`

**Browser Compatibility**:
- Chrome 64+
- Edge 79+
- Firefox 69+
- Safari 13.1+

### Date/Time Formatting Convention
- **Backend**: ALL date/time fields in VO classes MUST use `@JsonFormat` annotation for formatting
- **Frontend**: DO NOT format dates in JavaScript/Vue components - display backend-formatted values directly
- Backend annotation example:
  ```java
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
  private LocalDateTime createdTime;
  ```
- **Rationale**: Centralized formatting ensures consistency and reduces frontend code duplication

## Core Business Flows

### File Sync & Analysis Pipeline
1. Local files scanned/imported → `data_info` table (status=0)
2. `AnalysisService` triggers async analysis
3. Graph nodes execute:
   - `DataAnalysisNode` - Extracts question/root-cause/solution
   - `SessionExtractionNode` - Extracts key conversation fragments
   - `TagClassifyNode` - Auto-generates tags
4. Results written to `analysis_result` and `analysis_tag` tables

### Quiz Learning Loop
1. User creates `AnalysisCollection` with selected analysis results
2. "AI Learning Assistant" generates quiz questions via LLM
3. `QuizService` saves to `quiz_record` and `quiz_question` tables
4. Frontend renders questions via `QuestionRenderer.vue`
5. Answers submitted → statistics updated → achievement progress tracked

## Environment Requirements

- **JDK 17+**
- **Node.js v22+**
- **MySQL 8.0+** (database: `review_agent`)
- **Redis** (for caching/SSE)

### Environment Variables (Optional)
- `DB_USERNAME`, `DB_PASSWORD` - MySQL credentials (default: root/123456)
- `DASHSCOPE_API_KEY` - Alibaba DashScope API key
- `JWT_SECRET` - JWT signing secret (min 256 bits)
- `OLLAMA_BASE_URL`, `OLLAMA_CHAT_MODEL` - Local Ollama config

## Testing

Backend tests use Spring Boot Test framework:
- Located in `backend/src/test/java/`
- Run all: `mvn test`
- Run single test: `mvn test -Dtest=<ClassName>`

## Documentation

See `AGENTS.md` for comprehensive architecture documentation including:
- Version history and changelog
- Complete module index with data tables
- Mermaid diagrams for core workflows
- AppleStyle design specifications
- Technical debt tracking

## ROOT DIRECTORY DOCUMENT SPECIFICATION

The project root directory only allows the following three documents to be retained:
- `README.md` - Project Documentation (for Users)
- `CLAUDE.md` - Claude Code Development Guide (this document)
- `AGENTS.md` - Agent Development Guide
- `CHANGELOG.md` - Project Changelog

**All other documents must be placed under the 'docs/' directory**, Refer to the document directory structure that can be created
```
docs/
├── analysis/ # Problem analysis and troubleshooting report
├── reviews/ # Code review documentation
├── fixes/ # Fix summary report
├── architecture/ # Architecture design document
└── api/ # API documentation
```

### Document naming conventions

**Forced naming format**:
- 'YYYY-MM-DD-CASE-NNN-DESCRIPTION_VERSION.md' - e.g. '2026-01-15-CASE-001-DataAnalysisError_01.md'

**Naming Conventions**:
1. **Date section**: 'YYYY-MM-DD' (YEAR-MONTH-DAY), SEPARATED USING A HYPHEN
2. **CASE Number**: 'CASE-NNN' (Nth case/issue of the day)
- For example: 'CASE-001', 'CASE-002', 'CASE-003'
- Used to identify the first issue or case that was addressed that day
3. **Description Section**: Concise and clear description in Chinese or English
4. **Version Part**: '_V' or '_v' + Serial Number (two digits, less than zero)
- For example: '_01', '_02', '_03' or '_v1.0', '_v2.0'

**Example**:
- ✅ '2026-01-15-CASE-001-DATA_ANALYSIS_ERROR_01.md'
- ✅ '2026-01-15-CASE-002-MYSQL_CONNECTION_TIMEOUT_ISSUE_01.md'
- ❌ 'Data Synchronization Failure Analysis Report_0115.md' (Date, CASE Number Missing)
- ❌ '2026-01-15-Data Synchronization Failure Analysis_01.md' (CASE number missing)
- ❌ 'temp.md' (unclear)

**CASE Number Description**:
- Number each day starting from 'CASE-001'
- Incremental number for each issue or case handled on the same day
- Easy to track and correlate all issues handled on the day
- Example:
  - '2026-01-15-CASE-001-StackOverflow Fix_01.md'
  - '2026-01-15-CASE-002-MySQL connection timeout_01.md'
  - '2026-01-15-CASE-003-Log4j2 Configuration Fix_01.md'

### Document creation process
1. If there is no subdirectory name in the docs/ directory that matches the document requirements, create a new subdirectory according to the document category.
2. When creating a new document, place it directly in the 'docs/' directory or its subdirectory
3. Use the specification file name (including date/version)
4. Include the following meta information at the beginning of the document:
   ```markdown
   # Document title

   Creation Date: YYYY-MM-DD
   **Author**: xxx
   Version: v1.0
   **Status**: Draft/Under Review/Approved
   ```
   
### Consequences of Breaking the Rules

If you violate the document management rules:
- Documents other than 'README.md' and 'CLAUDE.md' appear in the root directory
- Document naming is not standardized
- Document organization is disorganized

**Consequences**:
- Code review will be rejected
- PRs cannot be merged
- Documents need to be refreshed for submission