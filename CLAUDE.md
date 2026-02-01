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
